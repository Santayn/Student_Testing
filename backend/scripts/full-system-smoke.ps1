param(
    [string]$BaseUrl = 'http://localhost:8081'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Net.Http
$client = [System.Net.Http.HttpClient]::new()
$client.Timeout = [TimeSpan]::FromSeconds(120)

$expectedOps = New-Object 'System.Collections.Generic.HashSet[string]'
$hitOps = New-Object 'System.Collections.Generic.HashSet[string]'
$stepResults = New-Object System.Collections.ArrayList

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function As-Array {
    param([object]$Value)

    if ($null -eq $Value) {
        Write-Output -NoEnumerate @()
        return
    }

    Write-Output -NoEnumerate @($Value)
}

function Convert-BodyToJson {
    param([object]$Body)

    if ($null -eq $Body) {
        return $null
    }

    return ($Body | ConvertTo-Json -Depth 32 -Compress)
}

function Invoke-HttpRequest {
    param(
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers,
        [string]$JsonBody,
        [System.Net.Http.HttpContent]$Content
    )

    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::new($Method.ToUpperInvariant()), $Url)
    if ($Headers) {
        foreach ($key in $Headers.Keys) {
            $null = $request.Headers.TryAddWithoutValidation($key, [string]$Headers[$key])
        }
    }

    $allowsBody = @('POST', 'PUT', 'PATCH') -contains $Method.ToUpperInvariant()
    if ($allowsBody) {
        if ($null -ne $Content) {
            $request.Content = $Content
        } elseif ($null -ne $JsonBody) {
            $request.Content = [System.Net.Http.StringContent]::new(
                $JsonBody,
                [System.Text.Encoding]::UTF8,
                'application/json'
            )
        }
    }

    $response = $client.SendAsync($request).GetAwaiter().GetResult()
    $text = if ($response.Content) {
        $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    } else {
        ''
    }

    $mediaType = ''
    if ($response.Content -and $response.Content.Headers.ContentType) {
        $mediaType = $response.Content.Headers.ContentType.MediaType
    }

    $json = $null
    if ($text -and $mediaType -like '*json*') {
        try {
            $json = $text | ConvertFrom-Json
        } catch {
            $json = $null
        }
    }

    return [pscustomobject]@{
        StatusCode     = [int]$response.StatusCode
        Text           = $text
        Json           = $json
        Headers        = $response.Headers
        ContentHeaders = if ($response.Content) { $response.Content.Headers } else { $null }
    }
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$CanonicalPath,
        [string]$Path,
        [object]$Body,
        [string]$Token,
        [int[]]$ExpectedStatus = @(200)
    )

    $headers = @{ Accept = 'application/json' }
    if ($Token) {
        $headers.Authorization = "Bearer $Token"
    }

    $response = Invoke-HttpRequest `
        -Method $Method `
        -Url ($BaseUrl.TrimEnd('/') + $Path) `
        -Headers $headers `
        -JsonBody (Convert-BodyToJson $Body) `
        -Content $null

    if ($CanonicalPath.StartsWith('/api/v1/')) {
        $null = $hitOps.Add(('{0} {1}' -f $Method.ToUpperInvariant(), $CanonicalPath))
    }

    if ($ExpectedStatus -notcontains $response.StatusCode) {
        $preview = if ($response.Text.Length -gt 400) {
            $response.Text.Substring(0, 400)
        } else {
            $response.Text
        }
        throw ('{0} {1} returned {2}. Expected: {3}. Body: {4}' -f
            $Method.ToUpperInvariant(),
            $Path,
            $response.StatusCode,
            ($ExpectedStatus -join ', '),
            $preview)
    }

    if ($null -ne $response.Json) {
        return $response.Json
    }

    if ($response.Text) {
        return $response.Text
    }

    return $null
}

function Invoke-MultipartApi {
    param(
        [string]$CanonicalPath,
        [string]$Path,
        [hashtable]$Fields,
        [string]$FileFieldName,
        [string]$FilePath,
        [string]$FileContentType,
        [string]$Token,
        [int[]]$ExpectedStatus = @(200)
    )

    $headers = @{ Accept = 'application/json' }
    if ($Token) {
        $headers.Authorization = "Bearer $Token"
    }

    $content = [System.Net.Http.MultipartFormDataContent]::new()
    foreach ($field in $Fields.Keys) {
        $content.Add(
            [System.Net.Http.StringContent]::new([string]$Fields[$field]),
            $field
        )
    }

    $fileBytes = [System.IO.File]::ReadAllBytes($FilePath)
    $fileContent = [System.Net.Http.ByteArrayContent]::new($fileBytes)
    $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse($FileContentType)
    $content.Add($fileContent, $FileFieldName, [System.IO.Path]::GetFileName($FilePath))

    $response = Invoke-HttpRequest `
        -Method 'POST' `
        -Url ($BaseUrl.TrimEnd('/') + $Path) `
        -Headers $headers `
        -JsonBody $null `
        -Content $content

    if ($CanonicalPath.StartsWith('/api/v1/')) {
        $null = $hitOps.Add(('POST {0}' -f $CanonicalPath))
    }

    if ($ExpectedStatus -notcontains $response.StatusCode) {
        $preview = if ($response.Text.Length -gt 400) {
            $response.Text.Substring(0, 400)
        } else {
            $response.Text
        }
        throw ('POST {0} returned {1}. Expected: {2}. Body: {3}' -f
            $Path,
            $response.StatusCode,
            ($ExpectedStatus -join ', '),
            $preview)
    }

    if ($null -ne $response.Json) {
        return $response.Json
    }

    return $response.Text
}

function Run-Step {
    param(
        [string]$Name,
        [scriptblock]$Action
    )

    Write-Host ('==> {0}' -f $Name)
    $startedAt = Get-Date
    try {
        $result = & $Action
        $duration = [math]::Round(((Get-Date) - $startedAt).TotalSeconds, 2)
        $null = $stepResults.Add([pscustomobject]@{
            step    = $Name
            status  = 'ok'
            seconds = $duration
        })
        Write-Host ('OK  {0} ({1}s)' -f $Name, $duration)
        return $result
    } catch {
        $duration = [math]::Round(((Get-Date) - $startedAt).TotalSeconds, 2)
        $null = $stepResults.Add([pscustomobject]@{
            step    = $Name
            status  = 'failed'
            seconds = $duration
            error   = $_.Exception.Message
        })
        throw
    }
}

function Find-ItemByProperty {
    param(
        [object]$Items,
        [string]$PropertyName,
        [object]$ExpectedValue
    )

    foreach ($item in (As-Array $Items)) {
        if ($null -ne $item -and $item.$PropertyName -eq $ExpectedValue) {
            return $item
        }
    }

    return $null
}

function New-MinimalDocx {
    param(
        [string]$TargetPath,
        [string[]]$Paragraphs
    )

    Add-Type -AssemblyName System.IO.Compression.FileSystem

    $root = Join-Path ([System.IO.Path]::GetTempPath()) ('docx-' + [guid]::NewGuid().ToString('N'))
    $relsDir = Join-Path $root '_rels'
    $wordDir = Join-Path $root 'word'
    $wordRelsDir = Join-Path $wordDir '_rels'

    $null = New-Item -ItemType Directory -Force -Path $relsDir
    $null = New-Item -ItemType Directory -Force -Path $wordRelsDir

    $utf8 = [System.Text.UTF8Encoding]::new($false)

    $documentParagraphs = foreach ($paragraph in $Paragraphs) {
        $escaped = [System.Security.SecurityElement]::Escape($paragraph)
        "<w:p><w:r><w:t xml:space=`"preserve`">$escaped</w:t></w:r></w:p>"
    }

    $documentXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    $($documentParagraphs -join [Environment]::NewLine)
    <w:sectPr>
      <w:pgSz w:w="12240" w:h="15840" />
      <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="708" w:footer="708" w:gutter="0" />
    </w:sectPr>
  </w:body>
</w:document>
"@

    [System.IO.File]::WriteAllText((Join-Path $root '[Content_Types].xml'), @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml" />
  <Default Extension="xml" ContentType="application/xml" />
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml" />
</Types>
"@, $utf8)

    [System.IO.File]::WriteAllText((Join-Path $relsDir '.rels'), @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml" />
</Relationships>
"@, $utf8)

    [System.IO.File]::WriteAllText((Join-Path $wordDir 'document.xml'), $documentXml, $utf8)
    [System.IO.File]::WriteAllText((Join-Path $wordRelsDir 'document.xml.rels'), @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships" />
"@, $utf8)

    if (Test-Path $TargetPath) {
        Remove-Item $TargetPath -Force
    }

    [System.IO.Compression.ZipFile]::CreateFromDirectory($root, $TargetPath)
    Remove-Item $root -Recurse -Force
}

try {
    $suffix = Get-Date -Format 'yyyyMMddHHmmss'
    $academicYear = [DateTime]::UtcNow.Year

    $state = [ordered]@{
        admin                = $null
        teacher              = $null
        student              = $null
        registeredUser       = $null
        newPersonId          = $null
        newUserId            = $null
        customRoleId         = $null
        customPermissionId   = $null
        facultyId            = $null
        groupId              = $null
        subjectId            = $null
        facultyMembershipId  = $null
        groupMembershipId    = $null
        subjectMembershipId  = $null
        templateId           = $null
        versionId            = $null
        lectureId            = $null
        loadTypeId           = $null
        subjectLoadTypeId    = $null
        teachingAssignmentId = $null
        enrollmentId         = $null
        lectureAssignmentId  = $null
        testId               = $null
        questionId           = $null
        correctOptionId      = $null
        wrongOptionId        = $null
        testAssignmentId     = $null
        directAttemptId      = $null
        publicAttemptId      = $null
    }

    Run-Step 'Load OpenAPI and expected operations' {
        $openApi = Invoke-HttpRequest `
            -Method 'GET' `
            -Url ($BaseUrl.TrimEnd('/') + '/v3/api-docs') `
            -Headers @{ Accept = 'application/json' } `
            -JsonBody $null `
            -Content $null

        Assert-True ($openApi.StatusCode -eq 200) 'OpenAPI document is not available.'
        Assert-True ($null -ne $openApi.Json) 'OpenAPI response is not JSON.'

        foreach ($pathNode in $openApi.Json.paths.PSObject.Properties) {
            if (-not $pathNode.Name.StartsWith('/api/v1/')) {
                continue
            }
            foreach ($methodNode in $pathNode.Value.PSObject.Properties) {
                $null = $expectedOps.Add(('{0} {1}' -f $methodNode.Name.ToUpperInvariant(), $pathNode.Name))
            }
        }

        Assert-True ($expectedOps.Count -gt 0) 'No versioned API operations were discovered in OpenAPI.'
    }

    Run-Step 'Page availability and unauthenticated checks' {
        $loginPage = Invoke-HttpRequest -Method 'GET' -Url ($BaseUrl.TrimEnd('/') + '/login.html') -Headers @{} -JsonBody $null -Content $null
        $swaggerPage = Invoke-HttpRequest -Method 'GET' -Url ($BaseUrl.TrimEnd('/') + '/swagger-ui.html') -Headers @{} -JsonBody $null -Content $null
        Assert-True ($loginPage.StatusCode -eq 200) 'login.html is not available.'
        Assert-True ($swaggerPage.StatusCode -eq 200) 'swagger-ui.html is not available.'

        $null = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/status' -Path '/api/v1/status' -ExpectedStatus @(401, 403)
        $null = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/users/me' -Path '/api/v1/users/me' -ExpectedStatus @(401, 403)
        $null = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/auth/login' -Path '/api/v1/auth/login' -Body @{
            login    = 'teacher'
            password = 'wrong-password'
        } -ExpectedStatus @(401, 403)
    }

    Run-Step 'Authenticate seeded users and inspect current profiles' {
        $state.admin = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/auth/login' -Path '/api/v1/auth/login' -Body @{
            login    = 'admin'
            password = 'admin123'
        } -ExpectedStatus @(200)
        $state.teacher = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/auth/login' -Path '/api/v1/auth/login' -Body @{
            login    = 'teacher'
            password = 'teacher1'
        } -ExpectedStatus @(200)
        $state.student = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/auth/login' -Path '/api/v1/auth/login' -Body @{
            login    = 'student'
            password = 'student1'
        } -ExpectedStatus @(200)

        $adminMe = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/auth/me' -Path '/api/v1/auth/me' -Token $state.admin.accessToken -ExpectedStatus @(200)
        $teacherMe = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/auth/me' -Path '/api/v1/auth/me' -Token $state.teacher.accessToken -ExpectedStatus @(200)
        $studentMe = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/auth/me' -Path '/api/v1/auth/me' -Token $state.student.accessToken -ExpectedStatus @(200)
        $null = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/status' -Path '/api/v1/status' -Token $state.admin.accessToken -ExpectedStatus @(200)

        $adminProfile = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/users/me' -Path '/api/v1/users/me' -Token $state.admin.accessToken -ExpectedStatus @(200)
        $teacherProfile = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/users/me' -Path '/api/v1/users/me' -Token $state.teacher.accessToken -ExpectedStatus @(200)
        $studentProfile = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/users/me' -Path '/api/v1/users/me' -Token $state.student.accessToken -ExpectedStatus @(200)

        Assert-True ($adminMe.login -eq 'admin') 'Admin auth/me returned unexpected login.'
        Assert-True ($teacherMe.login -eq 'teacher') 'Teacher auth/me returned unexpected login.'
        Assert-True ($studentMe.login -eq 'student') 'Student auth/me returned unexpected login.'
        Assert-True ($adminProfile.personId -gt 0) 'Admin profile does not have a personId.'
        Assert-True ($teacherProfile.personId -gt 0) 'Teacher profile does not have a personId.'
        Assert-True ($studentProfile.personId -gt 0) 'Student profile does not have a personId.'

        $state.admin | Add-Member -NotePropertyName personId -NotePropertyValue $adminProfile.personId
        $state.teacher | Add-Member -NotePropertyName personId -NotePropertyValue $teacherProfile.personId
        $state.student | Add-Member -NotePropertyName personId -NotePropertyValue $studentProfile.personId
    }

    Run-Step 'Roles, permissions, person management, registration, refresh, revoke, user activation and assignments' {
        $permissionName = ('smoke.permission.{0}' -f $suffix)
        $roleName = ('SMOKE_ROLE_{0}' -f $suffix)
        $personEmail = ('smoke.user.{0}@example.local' -f $suffix)
        $newLogin = ('smoke_user_{0}' -f $suffix)
        $password1 = ('SmokePass{0}!' -f $suffix.Substring($suffix.Length - 6))
        $password2 = ('SmokePass{0}?x' -f $suffix.Substring($suffix.Length - 6))

        $existingRoles = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/roles' -Path '/api/v1/roles' -Token $state.admin.accessToken -ExpectedStatus @(200)
        $existingPermissions = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/roles/permissions' -Path '/api/v1/roles/permissions' -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ((As-Array $existingRoles).Count -gt 0) 'Roles list is empty.'
        Assert-True ((As-Array $existingPermissions).Count -gt 0) 'Permissions list is empty.'

        $createdPermission = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/roles/permissions' -Path '/api/v1/roles/permissions' -Token $state.admin.accessToken -Body @{
            name        = $permissionName
            description = 'Smoke test permission'
        } -ExpectedStatus @(200, 201)
        $state.customPermissionId = $createdPermission.id
        Assert-True ($state.customPermissionId -gt 0) 'Permission was not created.'

        $createdRole = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/roles' -Path '/api/v1/roles' -Token $state.admin.accessToken -Body @{
            name        = $roleName
            description = 'Smoke test role'
        } -ExpectedStatus @(200, 201)
        $state.customRoleId = $createdRole.id
        Assert-True ($state.customRoleId -gt 0) 'Role was not created.'

        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/roles/{id}/permissions' -Path ('/api/v1/roles/{0}/permissions' -f $state.customRoleId) -Token $state.admin.accessToken -Body @{
            permissionIds = @($state.customPermissionId)
        } -ExpectedStatus @(200)

        $createdPerson = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/users/people' -Path '/api/v1/users/people' -Token $state.admin.accessToken -Body @{
            firstName   = 'Smoke'
            lastName    = 'User'
            dateOfBirth = '2001-02-03'
            email       = $personEmail
            phone       = '+1999000' + $suffix.Substring($suffix.Length - 4)
        } -ExpectedStatus @(200, 201)
        $state.newPersonId = $createdPerson.id
        Assert-True ($state.newPersonId -gt 0) 'Person was not created.'

        $personById = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/users/people/{personId}' -Path ('/api/v1/users/people/{0}' -f $state.newPersonId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($personById.email -eq $personEmail) 'Created person lookup returned unexpected data.'

        $updatedPerson = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/users/people/{personId}' -Path ('/api/v1/users/people/{0}' -f $state.newPersonId) -Token $state.admin.accessToken -Body @{
            firstName   = 'Smoke'
            lastName    = 'User Updated'
            dateOfBirth = '2001-02-03'
            email       = $personEmail
            phone       = '+1999000' + $suffix.Substring($suffix.Length - 4)
        } -ExpectedStatus @(200)
        Assert-True ($updatedPerson.lastName -eq 'User Updated') 'Person update did not persist.'

        $people = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/users/people' -Path '/api/v1/users/people' -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $people -PropertyName 'id' -ExpectedValue $state.newPersonId)) 'Created person is missing from people list.'

        $state.registeredUser = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/auth/register' -Path '/api/v1/auth/register' -Body @{
            login    = $newLogin
            password = $password1
            personId = $state.newPersonId
        } -ExpectedStatus @(201)
        Assert-True ([string]::IsNullOrWhiteSpace($state.registeredUser.accessToken) -eq $false) 'Registration did not return accessToken.'

        $registeredCurrent = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/auth/me' -Path '/api/v1/auth/me' -Token $state.registeredUser.accessToken -ExpectedStatus @(200)
        Assert-True ($registeredCurrent.login -eq $newLogin) 'Registered user auth/me returned unexpected login.'

        $null = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/auth/change-password' -Path '/api/v1/auth/change-password' -Token $state.registeredUser.accessToken -Body @{
            currentPassword = $password1
            newPassword     = $password2
        } -ExpectedStatus @(204)

        $null = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/auth/login' -Path '/api/v1/auth/login' -Body @{
            login    = $newLogin
            password = $password1
        } -ExpectedStatus @(401, 403)

        $state.registeredUser = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/auth/login' -Path '/api/v1/auth/login' -Body @{
            login    = $newLogin
            password = $password2
        } -ExpectedStatus @(200)

        $refreshedTokens = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/auth/refresh' -Path '/api/v1/auth/refresh' -Body @{
            refreshToken = $state.registeredUser.refreshToken
        } -ExpectedStatus @(200)
        Assert-True ([string]::IsNullOrWhiteSpace($refreshedTokens.refreshToken) -eq $false) 'Refresh did not return refreshToken.'

        $null = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/auth/revoke' -Path '/api/v1/auth/revoke' -Token $refreshedTokens.accessToken -Body @{
            refreshToken = $refreshedTokens.refreshToken
        } -ExpectedStatus @(204)

        $null = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/auth/refresh' -Path '/api/v1/auth/refresh' -Body @{
            refreshToken = $refreshedTokens.refreshToken
        } -ExpectedStatus @(400, 401, 403)

        $users = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/users' -Path '/api/v1/users' -Token $state.admin.accessToken -ExpectedStatus @(200)
        $newUser = Find-ItemByProperty -Items $users -PropertyName 'login' -ExpectedValue $newLogin
        Assert-True ($null -ne $newUser) 'Registered user is missing from users list.'
        $state.newUserId = $newUser.id

        $userById = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/users/{id}' -Path ('/api/v1/users/{0}' -f $state.newUserId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($userById.login -eq $newLogin) 'User lookup returned unexpected login.'

        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/users/{id}/active' -Path ('/api/v1/users/{0}/active' -f $state.newUserId) -Token $state.admin.accessToken -Body @{
            active = $false
        } -ExpectedStatus @(200)

        $null = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/auth/login' -Path '/api/v1/auth/login' -Body @{
            login    = $newLogin
            password = $password2
        } -ExpectedStatus @(401, 403)

        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/users/{id}/active' -Path ('/api/v1/users/{0}/active' -f $state.newUserId) -Token $state.admin.accessToken -Body @{
            active = $true
        } -ExpectedStatus @(200)

        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/users/{id}/roles' -Path ('/api/v1/users/{0}/roles' -f $state.newUserId) -Token $state.admin.accessToken -Body @{
            roleIds = @($state.customRoleId)
        } -ExpectedStatus @(200)

        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/users/{id}/permissions' -Path ('/api/v1/users/{0}/permissions' -f $state.newUserId) -Token $state.admin.accessToken -Body @{
            permissionIds = @($state.customPermissionId)
        } -ExpectedStatus @(200)

        $state.registeredUser = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/auth/login' -Path '/api/v1/auth/login' -Body @{
            login    = $newLogin
            password = $password2
        } -ExpectedStatus @(200)
    }

    Run-Step 'Reference data CRUD: faculties, groups, subjects, templates, versions, lectures' {
        $faculty = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/faculties' -Path '/api/v1/faculties' -Token $state.admin.accessToken -Body @{
            name        = ('Smoke Faculty ' + $suffix)
            code        = ('FAC' + $suffix)
            description = 'Smoke faculty'
        } -ExpectedStatus @(200, 201)
        $state.facultyId = $faculty.id

        $throwawayFaculty = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/faculties' -Path '/api/v1/faculties' -Token $state.admin.accessToken -Body @{
            name        = ('Throwaway Faculty ' + $suffix)
            code        = ('TF' + $suffix)
            description = 'Throwaway faculty'
        } -ExpectedStatus @(200, 201)

        $allFaculties = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/faculties' -Path '/api/v1/faculties' -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $allFaculties -PropertyName 'id' -ExpectedValue $state.facultyId)) 'Faculty is missing from faculties list.'

        $facultyById = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/faculties/{id}' -Path ('/api/v1/faculties/{0}' -f $state.facultyId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($facultyById.id -eq $state.facultyId) 'Faculty lookup returned wrong entity.'

        $updatedFaculty = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/faculties/{id}' -Path ('/api/v1/faculties/{0}' -f $state.facultyId) -Token $state.admin.accessToken -Body @{
            name        = ('Smoke Faculty Updated ' + $suffix)
            code        = ('FAC' + $suffix)
            description = 'Smoke faculty updated'
        } -ExpectedStatus @(200)
        Assert-True ($updatedFaculty.name -like 'Smoke Faculty Updated*') 'Faculty update did not persist.'

        $group = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/groups' -Path '/api/v1/groups' -Token $state.admin.accessToken -Body @{
            name      = ('Smoke Group ' + $suffix)
            code      = ('GRP' + $suffix)
            facultyId = $state.facultyId
        } -ExpectedStatus @(200, 201)
        $state.groupId = $group.id

        $throwawayGroup = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/groups' -Path '/api/v1/groups' -Token $state.admin.accessToken -Body @{
            name      = ('Throwaway Group ' + $suffix)
            code      = ('TGR' + $suffix)
            facultyId = $throwawayFaculty.id
        } -ExpectedStatus @(200, 201)

        $allGroups = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/groups' -Path '/api/v1/groups' -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $allGroups -PropertyName 'id' -ExpectedValue $state.groupId)) 'Group is missing from groups list.'

        $groupById = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/groups/{id}' -Path ('/api/v1/groups/{0}' -f $state.groupId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($groupById.id -eq $state.groupId) 'Group lookup returned wrong entity.'

        $updatedGroup = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/groups/{id}' -Path ('/api/v1/groups/{0}' -f $state.groupId) -Token $state.admin.accessToken -Body @{
            name      = ('Smoke Group Updated ' + $suffix)
            code      = ('GRP' + $suffix)
            facultyId = $state.facultyId
        } -ExpectedStatus @(200)
        Assert-True ($updatedGroup.name -like 'Smoke Group Updated*') 'Group update did not persist.'

        $subject = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/subjects' -Path '/api/v1/subjects' -Token $state.admin.accessToken -Body @{
            name        = ('Smoke Subject ' + $suffix)
            description = 'Smoke subject'
        } -ExpectedStatus @(200, 201)
        $state.subjectId = $subject.id

        $throwawaySubject = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/subjects' -Path '/api/v1/subjects' -Token $state.admin.accessToken -Body @{
            name        = ('Throwaway Subject ' + $suffix)
            description = 'Throwaway subject'
        } -ExpectedStatus @(200, 201)

        $allSubjects = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/subjects' -Path '/api/v1/subjects' -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $allSubjects -PropertyName 'id' -ExpectedValue $state.subjectId)) 'Subject is missing from subjects list.'

        $subjectById = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/subjects/{id}' -Path ('/api/v1/subjects/{0}' -f $state.subjectId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($subjectById.id -eq $state.subjectId) 'Subject lookup returned wrong entity.'

        $updatedSubject = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/subjects/{id}' -Path ('/api/v1/subjects/{0}' -f $state.subjectId) -Token $state.admin.accessToken -Body @{
            name        = ('Smoke Subject Updated ' + $suffix)
            description = 'Smoke subject updated'
        } -ExpectedStatus @(200)
        Assert-True ($updatedSubject.name -like 'Smoke Subject Updated*') 'Subject update did not persist.'

        $null = Invoke-Api -Method 'DELETE' -CanonicalPath '/api/v1/groups/{id}' -Path ('/api/v1/groups/{0}' -f $throwawayGroup.id) -Token $state.admin.accessToken -ExpectedStatus @(204)
        $null = Invoke-Api -Method 'DELETE' -CanonicalPath '/api/v1/faculties/{id}' -Path ('/api/v1/faculties/{0}' -f $throwawayFaculty.id) -Token $state.admin.accessToken -ExpectedStatus @(204)
        $null = Invoke-Api -Method 'DELETE' -CanonicalPath '/api/v1/subjects/{id}' -Path ('/api/v1/subjects/{0}' -f $throwawaySubject.id) -Token $state.admin.accessToken -ExpectedStatus @(204)

        $template = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/courses/templates' -Path '/api/v1/courses/templates' -Token $state.admin.accessToken -Body @{
            subjectId      = $state.subjectId
            authorPersonId = $state.teacher.personId
            name           = ('Smoke Course ' + $suffix)
            publicVisible  = $true
        } -ExpectedStatus @(200, 201)
        $state.templateId = $template.id

        $templateToDelete = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/courses/templates' -Path '/api/v1/courses/templates' -Token $state.admin.accessToken -Body @{
            subjectId      = $state.subjectId
            authorPersonId = $state.teacher.personId
            name           = ('Throwaway Course ' + $suffix)
            publicVisible  = $false
        } -ExpectedStatus @(200, 201)

        $templateList = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/courses/templates' -Path ('/api/v1/courses/templates?subjectId={0}&authorPersonId={1}' -f $state.subjectId, $state.teacher.personId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $templateList -PropertyName 'id' -ExpectedValue $state.templateId)) 'Template is missing from templates list.'

        $templateById = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/courses/templates/{templateId}' -Path ('/api/v1/courses/templates/{0}' -f $state.templateId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($templateById.id -eq $state.templateId) 'Template lookup returned wrong entity.'

        $updatedTemplate = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/courses/templates/{templateId}' -Path ('/api/v1/courses/templates/{0}' -f $state.templateId) -Token $state.admin.accessToken -Body @{
            subjectId      = $state.subjectId
            authorPersonId = $state.teacher.personId
            name           = ('Smoke Course Updated ' + $suffix)
            publicVisible  = $true
        } -ExpectedStatus @(200)
        Assert-True ($updatedTemplate.name -like 'Smoke Course Updated*') 'Template update did not persist.'

        $null = Invoke-Api -Method 'DELETE' -CanonicalPath '/api/v1/courses/templates/{templateId}' -Path ('/api/v1/courses/templates/{0}' -f $templateToDelete.id) -Token $state.admin.accessToken -ExpectedStatus @(204)

        $version = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/courses/templates/{templateId}/versions' -Path ('/api/v1/courses/templates/{0}/versions' -f $state.templateId) -Token $state.admin.accessToken -Body @{
            versionNumber       = 1
            title               = ('Smoke Version ' + $suffix)
            description         = 'Smoke course version'
            createdByPersonId   = $state.teacher.personId
            published           = $false
            publishedByPersonId = $null
            changeNotes         = 'Initial version'
        } -ExpectedStatus @(200, 201)
        $state.versionId = $version.id

        $versionList = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/courses/templates/{templateId}/versions' -Path ('/api/v1/courses/templates/{0}/versions' -f $state.templateId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $versionList -PropertyName 'id' -ExpectedValue $state.versionId)) 'Version is missing from versions list.'

        $versionById = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/courses/versions/{versionId}' -Path ('/api/v1/courses/versions/{0}' -f $state.versionId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($versionById.id -eq $state.versionId) 'Version lookup returned wrong entity.'

        $updatedVersion = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/courses/versions/{versionId}' -Path ('/api/v1/courses/versions/{0}' -f $state.versionId) -Token $state.admin.accessToken -Body @{
            versionNumber     = 1
            title             = ('Smoke Version Updated ' + $suffix)
            description       = 'Smoke course version updated'
            createdByPersonId = $state.teacher.personId
            changeNotes       = 'Updated version'
        } -ExpectedStatus @(200)
        Assert-True ($updatedVersion.title -like 'Smoke Version Updated*') 'Version update did not persist.'

        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/courses/versions/{versionId}/publish' -Path ('/api/v1/courses/versions/{0}/publish' -f $state.versionId) -Token $state.admin.accessToken -Body @{
            publishedByPersonId = $state.teacher.personId
        } -ExpectedStatus @(200)
        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/courses/versions/{versionId}/unpublish' -Path ('/api/v1/courses/versions/{0}/unpublish' -f $state.versionId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/courses/versions/{versionId}/publish' -Path ('/api/v1/courses/versions/{0}/publish' -f $state.versionId) -Token $state.admin.accessToken -Body @{
            publishedByPersonId = $state.teacher.personId
        } -ExpectedStatus @(200)

        $lecture = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/lectures' -Path '/api/v1/lectures' -Token $state.admin.accessToken -Body @{
            courseVersionId  = $state.versionId
            ordinal          = 1
            title            = ('Smoke Lecture ' + $suffix)
            description      = 'Smoke lecture'
            contentFolderKey = ('smoke-content-' + $suffix)
            publicVisible    = $true
        } -ExpectedStatus @(200, 201)
        $state.lectureId = $lecture.id

        $extraLecture = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/lectures' -Path '/api/v1/lectures' -Token $state.admin.accessToken -Body @{
            courseVersionId  = $state.versionId
            ordinal          = 2
            title            = ('Extra Lecture ' + $suffix)
            description      = 'Extra lecture'
            contentFolderKey = ('extra-content-' + $suffix)
            publicVisible    = $false
        } -ExpectedStatus @(200, 201)

        $lectureList = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/lectures' -Path ('/api/v1/lectures?courseVersionId={0}' -f $state.versionId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $lectureList -PropertyName 'id' -ExpectedValue $state.lectureId)) 'Lecture is missing from lectures list.'

        $lectureById = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/lectures/{id}' -Path ('/api/v1/lectures/{0}' -f $state.lectureId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($lectureById.id -eq $state.lectureId) 'Lecture lookup returned wrong entity.'

        $updatedLecture = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/lectures/{id}' -Path ('/api/v1/lectures/{0}' -f $state.lectureId) -Token $state.admin.accessToken -Body @{
            courseVersionId  = $state.versionId
            ordinal          = 1
            title            = ('Smoke Lecture Updated ' + $suffix)
            description      = 'Smoke lecture updated'
            contentFolderKey = ('smoke-content-' + $suffix + '-updated')
            publicVisible    = $true
        } -ExpectedStatus @(200)
        Assert-True ($updatedLecture.title -like 'Smoke Lecture Updated*') 'Lecture update did not persist.'

        $null = Invoke-Api -Method 'DELETE' -CanonicalPath '/api/v1/lectures/{id}' -Path ('/api/v1/lectures/{0}' -f $extraLecture.id) -Token $state.admin.accessToken -ExpectedStatus @(204)
    }

    Run-Step 'Membership CRUD for faculties, groups and subjects' {
        $facultyMemberships = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/memberships/faculties' -Path '/api/v1/memberships/faculties?activeOnly=true' -Token $state.admin.accessToken -ExpectedStatus @(200)
        $null = As-Array $facultyMemberships

        $facultyMembership = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/memberships/faculties/{facultyId}' -Path ('/api/v1/memberships/faculties/{0}' -f $state.facultyId) -Token $state.admin.accessToken -Body @{
            personId = $state.student.personId
            role     = 1
            notes    = 'Smoke faculty membership'
        } -ExpectedStatus @(200, 201)
        $state.facultyMembershipId = $facultyMembership.id

        $facultyMembershipsByFaculty = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/memberships/faculties/{facultyId}' -Path ('/api/v1/memberships/faculties/{0}?activeOnly=true' -f $state.facultyId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $facultyMembershipsByFaculty -PropertyName 'id' -ExpectedValue $state.facultyMembershipId)) 'Faculty membership is missing from faculty membership list.'

        $facultyMembershipById = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/memberships/faculties/memberships/{membershipId}' -Path ('/api/v1/memberships/faculties/memberships/{0}' -f $state.facultyMembershipId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($facultyMembershipById.id -eq $state.facultyMembershipId) 'Faculty membership lookup returned wrong entity.'

        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/memberships/faculties/memberships/{membershipId}' -Path ('/api/v1/memberships/faculties/memberships/{0}' -f $state.facultyMembershipId) -Token $state.admin.accessToken -Body @{
            status = 2
            notes  = 'Faculty membership paused'
        } -ExpectedStatus @(200)
        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/memberships/faculties/memberships/{membershipId}/status' -Path ('/api/v1/memberships/faculties/memberships/{0}/status' -f $state.facultyMembershipId) -Token $state.admin.accessToken -Body @{
            status = 1
        } -ExpectedStatus @(200)

        $groupMemberships = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/memberships/groups' -Path '/api/v1/memberships/groups?activeOnly=true' -Token $state.admin.accessToken -ExpectedStatus @(200)
        $null = As-Array $groupMemberships

        $groupMembership = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/memberships/groups/{groupId}' -Path ('/api/v1/memberships/groups/{0}' -f $state.groupId) -Token $state.admin.accessToken -Body @{
            personId = $state.student.personId
            role     = 1
            notes    = 'Smoke group membership'
        } -ExpectedStatus @(200, 201)
        $state.groupMembershipId = $groupMembership.id

        $groupMembershipsByGroup = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/memberships/groups/{groupId}' -Path ('/api/v1/memberships/groups/{0}?activeOnly=true' -f $state.groupId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $groupMembershipsByGroup -PropertyName 'id' -ExpectedValue $state.groupMembershipId)) 'Group membership is missing from group membership list.'

        $groupMembershipById = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/memberships/groups/memberships/{membershipId}' -Path ('/api/v1/memberships/groups/memberships/{0}' -f $state.groupMembershipId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($groupMembershipById.id -eq $state.groupMembershipId) 'Group membership lookup returned wrong entity.'

        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/memberships/groups/memberships/{membershipId}' -Path ('/api/v1/memberships/groups/memberships/{0}' -f $state.groupMembershipId) -Token $state.admin.accessToken -Body @{
            status = 2
            notes  = 'Group membership paused'
        } -ExpectedStatus @(200)
        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/memberships/groups/memberships/{membershipId}/status' -Path ('/api/v1/memberships/groups/memberships/{0}/status' -f $state.groupMembershipId) -Token $state.admin.accessToken -Body @{
            status = 1
        } -ExpectedStatus @(200)

        $subjectMemberships = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/memberships/subjects' -Path '/api/v1/memberships/subjects?activeOnly=true' -Token $state.admin.accessToken -ExpectedStatus @(200)
        $null = As-Array $subjectMemberships

        $subjectMembership = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/memberships/subjects/{subjectId}' -Path ('/api/v1/memberships/subjects/{0}' -f $state.subjectId) -Token $state.admin.accessToken -Body @{
            personId = $state.teacher.personId
            role     = 1
            notes    = 'Smoke subject membership'
        } -ExpectedStatus @(200, 201)
        $state.subjectMembershipId = $subjectMembership.id

        $subjectMembershipsBySubject = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/memberships/subjects/{subjectId}' -Path ('/api/v1/memberships/subjects/{0}?activeOnly=true' -f $state.subjectId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $subjectMembershipsBySubject -PropertyName 'id' -ExpectedValue $state.subjectMembershipId)) 'Subject membership is missing from subject membership list.'

        $subjectMembershipById = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/memberships/subjects/memberships/{membershipId}' -Path ('/api/v1/memberships/subjects/memberships/{0}' -f $state.subjectMembershipId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($subjectMembershipById.id -eq $state.subjectMembershipId) 'Subject membership lookup returned wrong entity.'

        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/memberships/subjects/memberships/{membershipId}' -Path ('/api/v1/memberships/subjects/memberships/{0}' -f $state.subjectMembershipId) -Token $state.admin.accessToken -Body @{
            status = 2
            notes  = 'Subject membership paused'
        } -ExpectedStatus @(200)
        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/memberships/subjects/memberships/{membershipId}/status' -Path ('/api/v1/memberships/subjects/memberships/{0}/status' -f $state.subjectMembershipId) -Token $state.admin.accessToken -Body @{
            status = 1
        } -ExpectedStatus @(200)
    }

    Run-Step 'Teaching load, subject-load-type, assignment, enrollment, lecture assignment and progress' {
        $loadTypes = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/teaching/load-types' -Path '/api/v1/teaching/load-types' -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ((As-Array $loadTypes).Count -gt 0) 'Load types list is empty.'

        $loadType = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/teaching/load-types' -Path '/api/v1/teaching/load-types' -Token $state.admin.accessToken -Body @{
            name        = ('Smoke Load Type ' + $suffix)
            description = 'Smoke load type'
        } -ExpectedStatus @(200, 201)
        $state.loadTypeId = $loadType.id

        $loadTypeById = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/teaching/load-types/{loadTypeId}' -Path ('/api/v1/teaching/load-types/{0}' -f $state.loadTypeId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($loadTypeById.id -eq $state.loadTypeId) 'Load type lookup returned wrong entity.'

        $updatedLoadType = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/teaching/load-types/{loadTypeId}' -Path ('/api/v1/teaching/load-types/{0}' -f $state.loadTypeId) -Token $state.admin.accessToken -Body @{
            name        = ('Smoke Load Type Updated ' + $suffix)
            description = 'Smoke load type updated'
        } -ExpectedStatus @(200)
        Assert-True ($updatedLoadType.name -like 'Smoke Load Type Updated*') 'Load type update did not persist.'

        $subjectLoadType = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/teaching/subject-memberships/{subjectMembershipId}/load-types' -Path ('/api/v1/teaching/subject-memberships/{0}/load-types' -f $state.subjectMembershipId) -Token $state.admin.accessToken -Body @{
            teachingLoadTypeId = $state.loadTypeId
            notes              = 'Smoke subject load type'
        } -ExpectedStatus @(200, 201)
        $state.subjectLoadTypeId = $subjectLoadType.id

        $subjectLoadTypes = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/teaching/subject-load-types' -Path ('/api/v1/teaching/subject-load-types?subjectMembershipId={0}&teachingLoadTypeId={1}' -f $state.subjectMembershipId, $state.loadTypeId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $subjectLoadTypes -PropertyName 'id' -ExpectedValue $state.subjectLoadTypeId)) 'Subject load type is missing from subject-load-types list.'

        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/teaching/subject-load-types/{subjectLoadTypeId}' -Path ('/api/v1/teaching/subject-load-types/{0}' -f $state.subjectLoadTypeId) -Token $state.admin.accessToken -Body @{
            status = 2
            notes  = 'Subject load type paused'
        } -ExpectedStatus @(200)
        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/teaching/subject-load-types/{subjectLoadTypeId}/status' -Path ('/api/v1/teaching/subject-load-types/{0}/status' -f $state.subjectLoadTypeId) -Token $state.admin.accessToken -Body @{
            status = 1
        } -ExpectedStatus @(200)

        $teachingAssignment = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/teaching/assignments' -Path '/api/v1/teaching/assignments' -Token $state.admin.accessToken -Body @{
            subjectMembershipId = $state.subjectMembershipId
            groupId             = $state.groupId
            loadTypeId          = $state.loadTypeId
            courseVersionId     = $state.versionId
            semester            = 1
            academicYear        = $academicYear
            hoursPerWeek        = 2.5
            status              = 1
            notes               = 'Smoke teaching assignment'
        } -ExpectedStatus @(200, 201)
        $state.teachingAssignmentId = $teachingAssignment.id

        $teachingAssignments = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/teaching/assignments' -Path ('/api/v1/teaching/assignments?groupId={0}&subjectMembershipId={1}&courseVersionId={2}&loadTypeId={3}' -f $state.groupId, $state.subjectMembershipId, $state.versionId, $state.loadTypeId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $teachingAssignments -PropertyName 'id' -ExpectedValue $state.teachingAssignmentId)) 'Teaching assignment is missing from assignments list.'

        $assignmentById = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/teaching/assignments/{id}' -Path ('/api/v1/teaching/assignments/{0}' -f $state.teachingAssignmentId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($assignmentById.id -eq $state.teachingAssignmentId) 'Teaching assignment lookup returned wrong entity.'

        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/teaching/assignments/{assignmentId}' -Path ('/api/v1/teaching/assignments/{0}' -f $state.teachingAssignmentId) -Token $state.admin.accessToken -Body @{
            subjectMembershipId = $state.subjectMembershipId
            groupId             = $state.groupId
            loadTypeId          = $state.loadTypeId
            courseVersionId     = $state.versionId
            semester            = 1
            academicYear        = $academicYear
            hoursPerWeek        = 3.0
            status              = 1
            notes               = 'Smoke teaching assignment updated'
        } -ExpectedStatus @(200)

        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/teaching/assignments/{assignmentId}/status' -Path ('/api/v1/teaching/assignments/{0}/status' -f $state.teachingAssignmentId) -Token $state.admin.accessToken -Body @{
            status = 2
        } -ExpectedStatus @(200)
        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/teaching/assignments/{assignmentId}/status' -Path ('/api/v1/teaching/assignments/{0}/status' -f $state.teachingAssignmentId) -Token $state.admin.accessToken -Body @{
            status = 1
        } -ExpectedStatus @(200)

        $enrollment = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/teaching/assignments/{assignmentId}/enrollments' -Path ('/api/v1/teaching/assignments/{0}/enrollments' -f $state.teachingAssignmentId) -Token $state.admin.accessToken -Body @{
            groupMembershipId = $state.groupMembershipId
            status            = 1
        } -ExpectedStatus @(200, 201)
        $state.enrollmentId = $enrollment.id

        $enrollments = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/teaching/enrollments' -Path ('/api/v1/teaching/enrollments?teachingAssignmentId={0}&groupMembershipId={1}&groupId={2}' -f $state.teachingAssignmentId, $state.groupMembershipId, $state.groupId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $enrollments -PropertyName 'id' -ExpectedValue $state.enrollmentId)) 'Enrollment is missing from enrollments list.'

        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/teaching/enrollments/{enrollmentId}/status' -Path ('/api/v1/teaching/enrollments/{0}/status' -f $state.enrollmentId) -Token $state.admin.accessToken -Body @{
            status = 2
        } -ExpectedStatus @(200)
        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/teaching/enrollments/{enrollmentId}/status' -Path ('/api/v1/teaching/enrollments/{0}/status' -f $state.enrollmentId) -Token $state.admin.accessToken -Body @{
            status = 1
        } -ExpectedStatus @(200)

        $now = [DateTime]::UtcNow
        $lectureAssignment = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/teaching/assignments/{assignmentId}/lecture-assignments' -Path ('/api/v1/teaching/assignments/{0}/lecture-assignments' -f $state.teachingAssignmentId) -Token $state.admin.accessToken -Body @{
            courseLectureId    = $state.lectureId
            availableFromUtc   = $now.AddHours(-1).ToString('o')
            dueToUtc           = $now.AddDays(7).ToString('o')
            closedAtUtc        = $null
            required           = $true
            minProgressPercent = 80
            status             = 1
        } -ExpectedStatus @(200, 201)
        $state.lectureAssignmentId = $lectureAssignment.id

        $lectureAssignments = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/teaching/lecture-assignments' -Path ('/api/v1/teaching/lecture-assignments?teachingAssignmentId={0}&courseLectureId={1}' -f $state.teachingAssignmentId, $state.lectureId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $lectureAssignments -PropertyName 'id' -ExpectedValue $state.lectureAssignmentId)) 'Lecture assignment is missing from lecture-assignments list.'

        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/teaching/lecture-assignments/{lectureAssignmentId}' -Path ('/api/v1/teaching/lecture-assignments/{0}' -f $state.lectureAssignmentId) -Token $state.admin.accessToken -Body @{
            courseLectureId    = $state.lectureId
            availableFromUtc   = $now.AddHours(-2).ToString('o')
            dueToUtc           = $now.AddDays(8).ToString('o')
            closedAtUtc        = $null
            required           = $true
            minProgressPercent = 75
            status             = 1
        } -ExpectedStatus @(200)

        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/teaching/lecture-assignments/{lectureAssignmentId}/status' -Path ('/api/v1/teaching/lecture-assignments/{0}/status' -f $state.lectureAssignmentId) -Token $state.admin.accessToken -Body @{
            status = 2
        } -ExpectedStatus @(200)
        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/teaching/lecture-assignments/{lectureAssignmentId}/status' -Path ('/api/v1/teaching/lecture-assignments/{0}/status' -f $state.lectureAssignmentId) -Token $state.admin.accessToken -Body @{
            status = 1
        } -ExpectedStatus @(200)

        $progress = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/teaching/lecture-assignments/{lectureAssignmentId}/progress' -Path ('/api/v1/teaching/lecture-assignments/{0}/progress' -f $state.lectureAssignmentId) -Token $state.admin.accessToken -Body @{
            teachingAssignmentEnrollmentId = $state.enrollmentId
            progressPercent                = 100
            status                         = 2
            completionSource               = 1
            completedByPersonId            = $state.student.personId
            lastPositionSeconds            = 600
            timeSpentSeconds               = 900
        } -ExpectedStatus @(200, 201)
        Assert-True ($progress.id -gt 0) 'Lecture progress was not created.'

        $progressRows = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/teaching/lecture-progress' -Path ('/api/v1/teaching/lecture-progress?lectureAssignmentId={0}&teachingAssignmentEnrollmentId={1}' -f $state.lectureAssignmentId, $state.enrollmentId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ((As-Array $progressRows).Count -ge 1) 'Lecture progress list is empty.'
    }

    Run-Step 'Tests, questions, options, import, assignments, attempts, responses and completion' {
        $test = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/tests' -Path '/api/v1/tests' -Token $state.admin.accessToken -Body @{
            title           = ('Smoke Test ' + $suffix)
            description     = 'Smoke test'
            duration        = $null
            attemptsAllowed = 5
            questionCount   = 1
            selectionRules  = @()
        } -ExpectedStatus @(200, 201)
        $state.testId = $test.id

        $tests = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/tests' -Path '/api/v1/tests' -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $tests -PropertyName 'id' -ExpectedValue $state.testId)) 'Test is missing from tests list.'

        $testById = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/tests/{id}' -Path ('/api/v1/tests/{0}' -f $state.testId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($testById.id -eq $state.testId) 'Test lookup returned wrong entity.'

        $updatedTest = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/tests/{id}' -Path ('/api/v1/tests/{0}' -f $state.testId) -Token $state.admin.accessToken -Body @{
            title           = ('Smoke Test Updated ' + $suffix)
            description     = 'Smoke test updated'
            duration        = $null
            attemptsAllowed = 5
            questionCount   = 1
            selectionRules  = @()
        } -ExpectedStatus @(200)
        Assert-True ($updatedTest.title -like 'Smoke Test Updated*') 'Test update did not persist.'

        $question = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/questions' -Path '/api/v1/questions' -Token $state.admin.accessToken -Body @{
            testId          = $state.testId
            courseLectureId = $state.lectureId
            type            = 2
            question        = 'Which answer is correct?'
            points          = 2
            ordinal         = 1
            correctAnswer   = $null
        } -ExpectedStatus @(200, 201)
        $state.questionId = $question.id

        $questionList = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/questions' -Path ('/api/v1/questions?testId={0}' -f $state.testId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $questionList -PropertyName 'id' -ExpectedValue $state.questionId)) 'Question is missing from questions list.'

        $questionById = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/questions/{questionId}' -Path ('/api/v1/questions/{0}' -f $state.questionId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($questionById.id -eq $state.questionId) 'Question lookup returned wrong entity.'

        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/questions/{questionId}' -Path ('/api/v1/questions/{0}' -f $state.questionId) -Token $state.admin.accessToken -Body @{
            courseLectureId = $state.lectureId
            type            = 2
            question        = 'Which answer is correct now?'
            points          = 2
            ordinal         = 1
            correctAnswer   = $null
            active          = $true
        } -ExpectedStatus @(200)

        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/questions/{questionId}/active' -Path ('/api/v1/questions/{0}/active' -f $state.questionId) -Token $state.admin.accessToken -Body @{
            active = $true
        } -ExpectedStatus @(200)

        $correctOption = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/questions/{questionId}/options' -Path ('/api/v1/questions/{0}/options' -f $state.questionId) -Token $state.admin.accessToken -Body @{
            text    = 'Correct option'
            ordinal = 1
            correct = $true
        } -ExpectedStatus @(200, 201)
        $state.correctOptionId = $correctOption.id

        $wrongOption = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/questions/{questionId}/options' -Path ('/api/v1/questions/{0}/options' -f $state.questionId) -Token $state.admin.accessToken -Body @{
            text    = 'Wrong option'
            ordinal = 2
            correct = $false
        } -ExpectedStatus @(200, 201)
        $state.wrongOptionId = $wrongOption.id

        $questionOptions = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/questions/{questionId}/options' -Path ('/api/v1/questions/{0}/options' -f $state.questionId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $questionOptions -PropertyName 'id' -ExpectedValue $state.correctOptionId)) 'Correct option is missing from options list.'

        $optionById = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/questions/options/{optionId}' -Path ('/api/v1/questions/options/{0}' -f $state.correctOptionId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($optionById.id -eq $state.correctOptionId) 'Option lookup returned wrong entity.'

        $updatedOption = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/questions/options/{optionId}' -Path ('/api/v1/questions/options/{0}' -f $state.correctOptionId) -Token $state.admin.accessToken -Body @{
            text    = 'Correct option updated'
            ordinal = 1
            correct = $true
        } -ExpectedStatus @(200)
        Assert-True ($updatedOption.text -eq 'Correct option updated') 'Option update did not persist.'
        $state.correctOptionId = $updatedOption.id

        $selectionRules = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/tests/{testId}/selection-rules' -Path ('/api/v1/tests/{0}/selection-rules' -f $state.testId) -Token $state.admin.accessToken -Body @(
            @{
                courseLectureId             = $state.lectureId
                questionCount               = 1
                textQuestionCount           = 0
                singleAnswerQuestionCount   = 1
                multipleAnswerQuestionCount = 0
                ordinal                     = 1
            }
        ) -ExpectedStatus @(200)
        Assert-True ((As-Array $selectionRules).Count -eq 1) 'Selection rules were not saved.'

        $selectionRulesList = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/tests/{testId}/selection-rules' -Path ('/api/v1/tests/{0}/selection-rules' -f $state.testId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ((As-Array $selectionRulesList).Count -eq 1) 'Selection rules list is empty.'

        $importDocxPath = Join-Path ([System.IO.Path]::GetTempPath()) ('smoke-import-' + $suffix + '.docx')
        New-MinimalDocx -TargetPath $importDocxPath -Paragraphs @(
            'Question 1: Select the best option',
            'Points: 2',
            'A) Wrong',
            'B) Right',
            'Answer: B',
            '',
            'Question 2: Type the keyword',
            'Answer: Java'
        )

        try {
            $importResult = Invoke-MultipartApi -CanonicalPath '/api/v1/questions/import' -Path '/api/v1/questions/import' -Token $state.admin.accessToken -Fields @{
                testId          = $state.testId
                courseLectureId = $state.lectureId
            } -FileFieldName 'file' -FilePath $importDocxPath -FileContentType 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' -ExpectedStatus @(200)
            Assert-True ($importResult.importedQuestions -ge 1) 'DOCX import did not create any questions.'
        } finally {
            if (Test-Path $importDocxPath) {
                Remove-Item $importDocxPath -Force
            }
        }

        $now = [DateTime]::UtcNow
        $assignment = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/tests/{testId}/assignments' -Path ('/api/v1/tests/{0}/assignments' -f $state.testId) -Token $state.admin.accessToken -Body @{
            scope             = 3
            courseVersionId   = $null
            courseLectureId   = $state.lectureId
            availableFromUtc  = $now.AddMinutes(-10).ToString('o')
            availableUntilUtc = $now.AddDays(1).ToString('o')
            status            = 2
        } -ExpectedStatus @(200, 201)
        $state.testAssignmentId = $assignment.id

        $assignments = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/tests/assignments' -Path ('/api/v1/tests/assignments?testId={0}&courseLectureId={1}' -f $state.testId, $state.lectureId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $assignments -PropertyName 'id' -ExpectedValue $state.testAssignmentId)) 'Test assignment is missing from assignments list.'

        $assignmentById = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/tests/assignments/{assignmentId}' -Path ('/api/v1/tests/assignments/{0}' -f $state.testAssignmentId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($assignmentById.id -eq $state.testAssignmentId) 'Test assignment lookup returned wrong entity.'

        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/tests/assignments/{assignmentId}' -Path ('/api/v1/tests/assignments/{0}' -f $state.testAssignmentId) -Token $state.admin.accessToken -Body @{
            scope             = 3
            courseVersionId   = $null
            courseLectureId   = $state.lectureId
            availableFromUtc  = $now.AddMinutes(-15).ToString('o')
            availableUntilUtc = $now.AddDays(2).ToString('o')
            status            = 2
        } -ExpectedStatus @(200)

        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/tests/assignments/{assignmentId}/status' -Path ('/api/v1/tests/assignments/{0}/status' -f $state.testAssignmentId) -Token $state.admin.accessToken -Body @{
            status = 1
        } -ExpectedStatus @(200)
        $null = Invoke-Api -Method 'PUT' -CanonicalPath '/api/v1/tests/assignments/{assignmentId}/status' -Path ('/api/v1/tests/assignments/{0}/status' -f $state.testAssignmentId) -Token $state.admin.accessToken -Body @{
            status = 2
        } -ExpectedStatus @(200)

        $directAttempt = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/tests/assignments/{assignmentId}/attempts' -Path ('/api/v1/tests/assignments/{0}/attempts' -f $state.testAssignmentId) -Token $state.student.accessToken -Body @{
            personId                      = $state.student.personId
            teachingAssignmentEnrollmentId = $state.enrollmentId
        } -ExpectedStatus @(200, 201)
        $state.directAttemptId = $directAttempt.id

        $attempts = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/tests/attempts' -Path ('/api/v1/tests/attempts?testAssignmentId={0}&personId={1}' -f $state.testAssignmentId, $state.student.personId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $attempts -PropertyName 'id' -ExpectedValue $state.directAttemptId)) 'Direct attempt is missing from attempts list.'

        $attemptById = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/tests/attempts/{attemptId}' -Path ('/api/v1/tests/attempts/{0}' -f $state.directAttemptId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ($attemptById.id -eq $state.directAttemptId) 'Attempt lookup returned wrong entity.'

        $responsesBefore = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/tests/attempts/{attemptId}/responses' -Path ('/api/v1/tests/attempts/{0}/responses' -f $state.directAttemptId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ((As-Array $responsesBefore).Count -eq 0) 'Direct attempt unexpectedly already had responses.'

        $submittedResponse = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/tests/attempts/{attemptId}/responses' -Path ('/api/v1/tests/attempts/{0}/responses' -f $state.directAttemptId) -Token $state.student.accessToken -Body @{
            testQuestionId    = $state.questionId
            answerText        = $null
            selectedOptionIds = @($state.correctOptionId)
            awardedPoints     = $null
            correct           = $null
        } -ExpectedStatus @(200, 201)
        Assert-True ($submittedResponse.id -gt 0) 'Response submission did not return response id.'

        $responsesAfter = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/tests/attempts/{attemptId}/responses' -Path ('/api/v1/tests/attempts/{0}/responses' -f $state.directAttemptId) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ((As-Array $responsesAfter).Count -ge 1) 'Responses list is empty after submission.'

        $selectedOptions = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/tests/responses/{responseId}/selected-options' -Path ('/api/v1/tests/responses/{0}/selected-options' -f $submittedResponse.id) -Token $state.admin.accessToken -ExpectedStatus @(200)
        Assert-True ((As-Array $selectedOptions).Count -eq 1) 'Selected option list does not contain the submitted option.'

        $completedAttempt = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/tests/attempts/{attemptId}/complete' -Path ('/api/v1/tests/attempts/{0}/complete' -f $state.directAttemptId) -Token $state.student.accessToken -Body @{
            status = 2
        } -ExpectedStatus @(200)
        Assert-True ($completedAttempt.status -eq 2) 'Attempt completion did not set completed status.'
    }

    Run-Step 'Public learning flows and teacher result views' {
        $publicSubject = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/public/learning/subjects/{subjectId}' -Path ('/api/v1/public/learning/subjects/{0}' -f $state.subjectId) -ExpectedStatus @(200)
        Assert-True ($publicSubject.id -eq $state.subjectId) 'Public subject lookup returned wrong entity.'

        $publicLectures = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/public/learning/subjects/{subjectId}/lectures' -Path ('/api/v1/public/learning/subjects/{0}/lectures' -f $state.subjectId) -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $publicLectures -PropertyName 'id' -ExpectedValue $state.lectureId)) 'Public lecture list does not contain the smoke lecture.'

        $publicLecture = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/public/learning/lectures/{lectureId}' -Path ('/api/v1/public/learning/lectures/{0}' -f $state.lectureId) -ExpectedStatus @(200)
        Assert-True ($publicLecture.id -eq $state.lectureId) 'Public lecture lookup returned wrong entity.'

        $publicTestsForLecture = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/public/learning/lectures/{lectureId}/tests' -Path ('/api/v1/public/learning/lectures/{0}/tests' -f $state.lectureId) -ExpectedStatus @(200)
        $publicTestRow = Find-ItemByProperty -Items $publicTestsForLecture -PropertyName 'assignmentId' -ExpectedValue $state.testAssignmentId
        Assert-True ($null -ne $publicTestRow) 'Public lecture tests do not contain the active test assignment.'

        $publicTest = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/public/learning/tests/{testId}' -Path ('/api/v1/public/learning/tests/{0}' -f $state.testId) -ExpectedStatus @(200)
        Assert-True ((As-Array $publicTest.questions).Count -ge 1) 'Public test lookup returned no questions.'

        $startedPublicAttempt = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/public/learning/test-assignments/{assignmentId}/attempts/start' -Path ('/api/v1/public/learning/test-assignments/{0}/attempts/start' -f $state.testAssignmentId) -Token $state.student.accessToken -ExpectedStatus @(200, 201)
        $state.publicAttemptId = $startedPublicAttempt.attemptId
        Assert-True ($state.publicAttemptId -gt 0) 'Public attempt start did not return attemptId.'

        $publicQuestion = (As-Array $startedPublicAttempt.questions | Select-Object -First 1)
        Assert-True ($null -ne $publicQuestion) 'Started public attempt returned no questions.'

        $publicAttemptSubmit = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/public/learning/attempts/{attemptId}/submit' -Path ('/api/v1/public/learning/attempts/{0}/submit' -f $state.publicAttemptId) -Token $state.student.accessToken -Body @{
            questionIds       = @($publicQuestion.id)
            answers           = @($null)
            selectedOptionIds = @(@($state.correctOptionId))
        } -ExpectedStatus @(200, 201)
        Assert-True ($publicAttemptSubmit.correctCount -ge 1) 'Submitting started public attempt did not mark the correct answer.'

        $publicDirectSubmit = Invoke-Api -Method 'POST' -CanonicalPath '/api/v1/public/learning/tests/{testId}/submit' -Path ('/api/v1/public/learning/tests/{0}/submit' -f $state.testId) -Token $state.student.accessToken -Body @{
            questionIds       = @($state.questionId)
            answers           = @($null)
            selectedOptionIds = @(@($state.correctOptionId))
        } -ExpectedStatus @(200, 201)
        Assert-True ($publicDirectSubmit.correctCount -ge 1) 'Direct public test submission did not mark the correct answer.'

        $teacherSubjects = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/results/teacher/subjects' -Path '/api/v1/results/teacher/subjects' -Token $state.teacher.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $teacherSubjects -PropertyName 'id' -ExpectedValue $state.subjectId)) 'Teacher subjects do not contain the smoke subject.'

        $teacherLectures = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/results/teacher/lectures' -Path ('/api/v1/results/teacher/lectures?subjectId={0}' -f $state.subjectId) -Token $state.teacher.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $teacherLectures -PropertyName 'id' -ExpectedValue $state.lectureId)) 'Teacher lectures do not contain the smoke lecture.'

        $teacherTests = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/results/teacher/tests' -Path ('/api/v1/results/teacher/tests?lectureId={0}' -f $state.lectureId) -Token $state.teacher.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $teacherTests -PropertyName 'id' -ExpectedValue $state.testId)) 'Teacher tests do not contain the smoke test.'

        $teacherGroups = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/results/teacher/groups' -Path ('/api/v1/results/teacher/groups?testId={0}' -f $state.testId) -Token $state.teacher.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $teacherGroups -PropertyName 'id' -ExpectedValue $state.groupId)) 'Teacher groups do not contain the smoke group.'

        $teacherStudents = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/results/teacher/students' -Path ('/api/v1/results/teacher/students?groupId={0}' -f $state.groupId) -Token $state.teacher.accessToken -ExpectedStatus @(200)
        Assert-True ($null -ne (Find-ItemByProperty -Items $teacherStudents -PropertyName 'id' -ExpectedValue $state.student.personId)) 'Teacher students do not contain the smoke student.'

        $teacherData = Invoke-Api -Method 'GET' -CanonicalPath '/api/v1/results/teacher/data' -Path ('/api/v1/results/teacher/data?subjectId={0}&lectureId={1}&testId={2}&groupId={3}&studentId={4}' -f $state.subjectId, $state.lectureId, $state.testId, $state.groupId, $state.student.personId) -Token $state.teacher.accessToken -ExpectedStatus @(200)
        Assert-True ($teacherData.stats.total -ge 1) 'Teacher result data does not contain any completed answers.'
    }

    $missingOps = New-Object System.Collections.ArrayList
    foreach ($operation in $expectedOps) {
        if (-not $hitOps.Contains($operation)) {
            $null = $missingOps.Add($operation)
        }
    }

    $summary = [pscustomobject]@{
        baseUrl            = $BaseUrl
        expectedOperations = $expectedOps.Count
        executedOperations = $hitOps.Count
        missingOperations  = @($missingOps)
        createdEntities    = [pscustomobject]@{
            newUserId        = $state.newUserId
            facultyId        = $state.facultyId
            groupId          = $state.groupId
            subjectId        = $state.subjectId
            templateId       = $state.templateId
            versionId        = $state.versionId
            lectureId        = $state.lectureId
            testId           = $state.testId
            testAssignmentId = $state.testAssignmentId
            directAttemptId  = $state.directAttemptId
            publicAttemptId  = $state.publicAttemptId
        }
        steps = $stepResults
    }

    $summary | ConvertTo-Json -Depth 8

    if ($missingOps.Count -gt 0) {
        throw ('Not all /api/v1 operations were exercised. Missing: {0}' -f ($missingOps -join '; '))
    }
} finally {
    $client.Dispose()
}

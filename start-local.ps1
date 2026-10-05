param(
    [switch]$NoBuild
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

function New-HexSecret {
    param(
        [Parameter(Mandatory = $true)]
        [int]$ByteCount
    )

    $bytes = New-Object byte[] $ByteCount
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()

    try {
        $generator.GetBytes($bytes)
    }
    finally {
        $generator.Dispose()
    }

    return (($bytes | ForEach-Object { $_.ToString("x2") }) -join "")
}

function Get-EnvironmentValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Content,

        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    $escapedName = [Regex]::Escape($Name)
    $match = [Regex]::Match(
        $Content,
        "(?m)^\s*$escapedName\s*=\s*(.*?)\s*$"
    )

    if (-not $match.Success) {
        return $null
    }

    return $match.Groups[1].Value.Trim()
}

function Set-EnvironmentValue {
    param(
        [Parameter(Mandatory = $true)]
        [AllowEmptyString()]
        [string]$Content,

        [Parameter(Mandatory = $true)]
        [string]$Name,

        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    $escapedName = [Regex]::Escape($Name)
    $pattern = "(?m)^\s*$escapedName\s*=.*$"
    $replacement = "$Name=$Value"

    if ([Regex]::IsMatch($Content, $pattern)) {
        return [Regex]::Replace($Content, $pattern, $replacement, 1)
    }

    if ([string]::IsNullOrWhiteSpace($Content)) {
        return "$replacement`r`n"
    }

    return $Content.TrimEnd("`r", "`n") + "`r`n$replacement`r`n"
}

$environmentPath = Join-Path $projectRoot ".env"
$environmentExisted = Test-Path $environmentPath
$environmentContent = if ($environmentExisted) {
    [System.IO.File]::ReadAllText($environmentPath)
}
else {
    ""
}

$databasePassword = Get-EnvironmentValue `
    -Content $environmentContent `
    -Name "POSTGRES_PASSWORD"
$jwtSecret = Get-EnvironmentValue `
    -Content $environmentContent `
    -Name "APP_JWT_SECRET"

$needsDatabasePassword = [string]::IsNullOrWhiteSpace($databasePassword) -or $databasePassword.Length -lt 12
$needsJwtSecret = [string]::IsNullOrWhiteSpace($jwtSecret) -or $jwtSecret.Length -lt 32

if ($needsDatabasePassword -or $needsJwtSecret) {
    if ($environmentExisted) {
        $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
        $backupPath = Join-Path $projectRoot ".env.backup-$timestamp"
        Copy-Item $environmentPath $backupPath
        Write-Host "Старый .env сохранён как $backupPath" -ForegroundColor Yellow
    }

    if ($needsDatabasePassword) {
        $environmentContent = Set-EnvironmentValue `
            -Content $environmentContent `
            -Name "POSTGRES_PASSWORD" `
            -Value (New-HexSecret -ByteCount 24)
    }

    if ($needsJwtSecret) {
        $environmentContent = Set-EnvironmentValue `
            -Content $environmentContent `
            -Name "APP_JWT_SECRET" `
            -Value (New-HexSecret -ByteCount 48)
    }

    $utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText(
        $environmentPath,
        $environmentContent,
        $utf8WithoutBom
    )

    if ($environmentExisted) {
        Write-Host "Локальный .env обновлён: изменены только отсутствующие/некорректные секреты." -ForegroundColor Green
    }
    else {
        Write-Host "Создан минимальный локальный .env с новыми секретами." -ForegroundColor Green
    }
}

$dockerCommand = Get-Command docker -ErrorAction SilentlyContinue

if (-not $dockerCommand) {
    throw "Docker не найден. Установите и запустите Docker Desktop."
}

$composeArguments = @("compose", "up", "-d")

if (-not $NoBuild) {
    $composeArguments += "--build"
}

& docker @composeArguments

if ($LASTEXITCODE -ne 0) {
    throw "Docker Compose завершился с ошибкой $LASTEXITCODE."
}

Write-Host ""
Write-Host "Student Test запущен: http://localhost/" -ForegroundColor Green
Write-Host "Состояние контейнеров: docker compose ps"

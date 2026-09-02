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

function Test-EnvironmentValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Content,

        [Parameter(Mandatory = $true)]
        [string]$Name,

        [Parameter(Mandatory = $true)]
        [int]$MinimumLength
    )

    $escapedName = [Regex]::Escape($Name)
    $match = [Regex]::Match(
        $Content,
        "(?m)^\s*$escapedName\s*=\s*(.+?)\s*$"
    )

    if (-not $match.Success) {
        return $false
    }

    $value = $match.Groups[1].Value.Trim()
    return $value.Length -ge $MinimumLength
}

$environmentPath = Join-Path $projectRoot ".env"
$mustCreateEnvironment = -not (Test-Path $environmentPath)

if (-not $mustCreateEnvironment) {
    $environmentContent = [System.IO.File]::ReadAllText($environmentPath)
    $hasDatabasePassword = Test-EnvironmentValue `
        -Content $environmentContent `
        -Name "POSTGRES_PASSWORD" `
        -MinimumLength 12
    $hasJwtSecret = Test-EnvironmentValue `
        -Content $environmentContent `
        -Name "APP_JWT_SECRET" `
        -MinimumLength 32

    $mustCreateEnvironment = -not ($hasDatabasePassword -and $hasJwtSecret)

    if ($mustCreateEnvironment) {
        $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
        $backupPath = Join-Path $projectRoot ".env.backup-$timestamp"
        Copy-Item $environmentPath $backupPath
        Write-Host "Старый некорректный .env сохранён как $backupPath" -ForegroundColor Yellow
    }
}

if ($mustCreateEnvironment) {
    $databasePassword = New-HexSecret -ByteCount 24
    $jwtSecret = New-HexSecret -ByteCount 48
    $environmentContent = @"
POSTGRES_DB=student_test
POSTGRES_USER=student_test
POSTGRES_PASSWORD=$databasePassword

SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_SQL_INIT_MODE=always
APP_DATA_LOADER_ENABLED=true
APP_PUBLIC_REGISTRATION_ENABLED=false
APP_JWT_SECRET=$jwtSecret
APP_CORS_ALLOWED_ORIGINS=http://localhost:[*],http://127.0.0.1:[*]
"@

    $utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText(
        $environmentPath,
        $environmentContent,
        $utf8WithoutBom
    )

    Write-Host "Создан новый локальный .env." -ForegroundColor Green
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

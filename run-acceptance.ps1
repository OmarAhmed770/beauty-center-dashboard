[CmdletBinding()]
param(
    [switch]$UseDockerPostgres,
    [switch]$RemoveDockerPostgres
)

$ErrorActionPreference = 'Stop'
$acceptanceEnvironmentFile = Join-Path $PSScriptRoot '.env.acceptance.local'

function Import-AcceptanceEnvironment {
    param([Parameter(Mandatory = $true)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Acceptance environment file not found. Copy .env.acceptance.example to .env.acceptance.local and fill its placeholders privately."
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmedLine = $line.Trim()
        if ($trimmedLine.Length -eq 0 -or $trimmedLine.StartsWith('#')) {
            continue
        }

        $separatorIndex = $trimmedLine.IndexOf('=')
        if ($separatorIndex -le 0) {
            throw 'The acceptance environment file contains an invalid line. Use NAME=value syntax.'
        }

        $name = $trimmedLine.Substring(0, $separatorIndex).Trim()
        $value = $trimmedLine.Substring($separatorIndex + 1)
        if ($name -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
            throw 'The acceptance environment file contains an invalid variable name.'
        }
        Set-Item -Path "Env:$name" -Value $value
    }
}

function Assert-AcceptanceVariables {
    param([string[]]$Names)

    $missing = foreach ($name in $Names) {
        $variable = Get-Item -Path "Env:$name" -ErrorAction SilentlyContinue
        if ($null -eq $variable -or [string]::IsNullOrWhiteSpace($variable.Value)) {
            $name
        }
    }

    if ($missing.Count -gt 0) {
        throw "Required acceptance variables are missing: $($missing -join ', '). Values were not displayed."
    }
}

function Invoke-AcceptanceDockerCompose {
    param([Parameter(Mandatory = $true)][string[]]$ComposeArguments)

    # Docker Compose interpolates $NAME in an env file. Escape dollar signs in a
    # short-lived copy so legitimate private passwords are passed literally.
    $dockerEnvironmentFile = Join-Path ([System.IO.Path]::GetTempPath()) ("beauty-center-acceptance-$([Guid]::NewGuid()).env")
    try {
        $dockerEnvironmentLines = Get-Content -LiteralPath $acceptanceEnvironmentFile | ForEach-Object {
            $_.Replace('$', '$$')
        }
        [System.IO.File]::WriteAllLines(
            $dockerEnvironmentFile,
            [string[]]$dockerEnvironmentLines,
            [System.Text.UTF8Encoding]::new($false))

        & docker compose --project-name beauty-center-acceptance --env-file $dockerEnvironmentFile @ComposeArguments
        if ($LASTEXITCODE -ne 0) {
            throw 'The isolated acceptance Docker Compose command failed.'
        }
    } finally {
        if (Test-Path -LiteralPath $dockerEnvironmentFile) {
            Remove-Item -LiteralPath $dockerEnvironmentFile -Force
        }
    }
}

function Assert-LocalPortAvailable {
    param(
        [Parameter(Mandatory = $true)][string]$Port,
        [Parameter(Mandatory = $true)][string]$ServiceName
    )

    $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -ne $listener) {
        throw "$ServiceName cannot start because local port $Port is already in use. Stop the conflicting local process and run the launcher again."
    }
}

Import-AcceptanceEnvironment -Path $acceptanceEnvironmentFile

if ($UseDockerPostgres -and $RemoveDockerPostgres) {
    throw 'Use either -UseDockerPostgres or -RemoveDockerPostgres, not both.'
}

$requiredVariables = @(
    'ACCEPTANCE_DB_URL',
    'ACCEPTANCE_DB_USERNAME',
    'ACCEPTANCE_DB_PASSWORD',
    'ACCEPTANCE_JWT_SECRET',
    'ACCEPTANCE_SEED_ENABLED',
    'ACCEPTANCE_OWNER_USERNAME',
    'ACCEPTANCE_OWNER_PASSWORD',
    'ACCEPTANCE_MANAGER_USERNAME',
    'ACCEPTANCE_MANAGER_PASSWORD',
    'ACCEPTANCE_RECEPTIONIST_USERNAME',
    'ACCEPTANCE_RECEPTIONIST_PASSWORD'
)
Assert-AcceptanceVariables -Names $requiredVariables

if ($env:ACCEPTANCE_SEED_ENABLED -ine 'true') {
    throw 'ACCEPTANCE_SEED_ENABLED must be true for the isolated acceptance launcher.'
}

if ($env:ACCEPTANCE_DB_URL -notmatch '(?i)acceptance') {
    throw 'ACCEPTANCE_DB_URL must name an isolated database containing "acceptance". No service was started.'
}

if ([Text.Encoding]::UTF8.GetByteCount($env:ACCEPTANCE_JWT_SECRET) -lt 32) {
    throw 'ACCEPTANCE_JWT_SECRET must contain at least 32 UTF-8 bytes. Its value was not displayed.'
}

if ($RemoveDockerPostgres) {
    Assert-AcceptanceVariables -Names @('POSTGRES_DB', 'POSTGRES_USER', 'POSTGRES_PASSWORD', 'POSTGRES_PORT')
    Invoke-AcceptanceDockerCompose -ComposeArguments @('down', '-v')
    Write-Host 'Removed only the beauty-center-acceptance Docker container and its isolated volume.'
    return
}

$backendDirectory = Join-Path $PSScriptRoot 'backend'
$frontendDirectory = Join-Path $PSScriptRoot 'frontend'
if (-not (Test-Path -LiteralPath $backendDirectory -PathType Container) -or -not (Test-Path -LiteralPath $frontendDirectory -PathType Container)) {
    throw 'Expected backend and frontend directories were not found. No service was started.'
}

$backendPort = if ($env:ACCEPTANCE_SERVER_PORT) { $env:ACCEPTANCE_SERVER_PORT } else { '8081' }
Assert-LocalPortAvailable -Port $backendPort -ServiceName 'Acceptance backend'
Assert-LocalPortAvailable -Port '5173' -ServiceName 'Acceptance frontend'

if ($UseDockerPostgres) {
    Assert-AcceptanceVariables -Names @('POSTGRES_DB', 'POSTGRES_USER', 'POSTGRES_PASSWORD', 'POSTGRES_PORT')
    Invoke-AcceptanceDockerCompose -ComposeArguments @('up', '-d', 'postgres')
}

$env:VITE_API_BASE_URL = if ($env:ACCEPTANCE_VITE_API_BASE_URL) {
    $env:ACCEPTANCE_VITE_API_BASE_URL
} else {
    $fallbackBackendPort = if ($env:ACCEPTANCE_SERVER_PORT) { $env:ACCEPTANCE_SERVER_PORT } else { '8081' }
    "http://127.0.0.1:$fallbackBackendPort"
}

$backendProcess = Start-Process -FilePath 'mvn.cmd' -ArgumentList @('spring-boot:run', '-Dspring-boot.run.profiles=acceptance') -WorkingDirectory $backendDirectory -WindowStyle Hidden -PassThru
$frontendProcess = Start-Process -FilePath 'npm.cmd' -ArgumentList @('run', 'dev', '--', '--host', '127.0.0.1', '--port', '5173', '--strictPort') -WorkingDirectory $frontendDirectory -WindowStyle Hidden -PassThru

Write-Host "Acceptance backend: http://127.0.0.1:$backendPort"
Write-Host 'Acceptance frontend: http://127.0.0.1:5173'
Write-Host "Backend process ID: $($backendProcess.Id)"
Write-Host "Frontend process ID: $($frontendProcess.Id)"
Write-Host 'Use Stop-Process with the shown process IDs to stop the local acceptance services.'

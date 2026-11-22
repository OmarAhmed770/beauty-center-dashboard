param([switch]$Seed, [string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$privateFile = Join-Path $root '.env.demo.local'
if (-not (Test-Path $privateFile)) { throw 'Run tools/demo/provision-demo.ps1 first.' }
foreach ($line in Get-Content -LiteralPath $privateFile) {
    if ($line -match '^(DEMO_[A-Z_]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process') }
}
if ($env:DEMO_DB_URL -notmatch '^jdbc:postgresql://(127\.0\.0\.1|localhost):[0-9]+/beauty_center_demo$') { throw 'Unsafe demo connection refused.' }
if (-not $JavaHome) { throw 'Pass -JavaHome pointing to JDK 17. Global Java settings are not changed.' }
$java = Join-Path $JavaHome 'bin\java.exe'
if (-not (Test-Path -LiteralPath $java)) { throw "Java executable not found: $java" }
$version = cmd.exe /d /c "`"$java`" -version 2>&1" | Out-String
if ($version -notmatch 'version "17\.') { throw 'The demo requires Java 17.' }
$jar = Join-Path $root 'backend/target/beauty-center-backend-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path $jar)) { throw 'Build the backend JAR before starting the demo.' }
foreach ($port in @(8082, 5175)) {
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { throw "Demo port $port is busy. Existing services were not stopped." }
}
$runtime = Join-Path $root 'tmp/demo'
New-Item -ItemType Directory -Force -Path $runtime | Out-Null
$env:SPRING_PROFILES_ACTIVE = 'demo'
$env:DEMO_SERVER_PORT = '8082'
$env:DEMO_BACKUP_DIRECTORY = Join-Path $runtime 'backups'
$env:DEMO_READ_ONLY = if ($Seed) { 'false' } else { 'true' }
function Start-DemoBackend {
    $stamp = [Guid]::NewGuid().ToString('N')
    $process = Start-Process -FilePath $java -ArgumentList @('-jar', "`"$jar`"") -WorkingDirectory $runtime -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtime "backend-$stamp.log") -RedirectStandardError (Join-Path $runtime "backend-$stamp-error.log")
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        if ($process.HasExited) { throw 'Demo backend stopped; check tmp/demo logs (do not publish them).' }
        try { $null = Invoke-RestMethod 'http://127.0.0.1:8082/api/v1/setup/status' -TimeoutSec 2; return $process } catch { Start-Sleep -Seconds 1 }
    }
    Stop-Process -Id $process.Id
    throw 'Demo backend did not become ready.'
}
$backend = Start-DemoBackend
if ($Seed) {
    try { & (Join-Path $root 'tools/demo/seed-demo.ps1') } finally { if (-not $backend.HasExited) { Stop-Process -Id $backend.Id; $backend.WaitForExit() } }
    $env:DEMO_READ_ONLY = 'true'
    $backend = Start-DemoBackend
}
$env:VITE_API_BASE_URL = 'http://127.0.0.1:8082'
$env:VITE_DEMO_READ_ONLY = 'true'
$frontend = Start-Process -FilePath 'cmd.exe' -ArgumentList @('/d', '/c', 'npm.cmd run dev -- --host 127.0.0.1 --port 5175 --strictPort') -WorkingDirectory (Join-Path $root 'frontend') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtime 'frontend.log') -RedirectStandardError (Join-Path $runtime 'frontend-error.log')
Write-Output "Demo backend PID=$($backend.Id); frontend PID=$($frontend.Id). Local demo: http://127.0.0.1:5175 . Business writes blocked; client launcher and configuration untouched."

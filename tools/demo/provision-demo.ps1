param([string]$ContainerName = 'beautycentermanagementsystem-postgres-1', [int]$Port = 5433)
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$privateFile = Join-Path $root '.env.demo.local'
if (Test-Path -LiteralPath $privateFile) { throw 'Demo configuration already exists; refusing to replace it.' }
function Secret {
    $bytes = New-Object byte[] 36
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    [Convert]::ToBase64String($bytes)
}
$existing = 'SELECT count(*) FROM pg_database WHERE datname = ''beauty_center_demo'';' | docker exec -i $ContainerName sh -c 'psql -X -At -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres'
if ($LASTEXITCODE -ne 0 -or $existing.Trim() -ne '0') { throw 'Cannot provision: demo database exists or PostgreSQL is unavailable.' }
$role = 'SELECT count(*) FROM pg_roles WHERE rolname = ''beauty_demo_app'';' | docker exec -i $ContainerName sh -c 'psql -X -At -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres'
if ($LASTEXITCODE -ne 0 -or $role.Trim() -ne '0') { throw 'Demo role already exists; refusing to change its credentials.' }
$databasePassword = Secret
$jwt = Secret
$ownerPassword = Secret
$staffPassword = Secret
# Newly generated DEMO credentials only. This script never reads the client's .env.
$lines = @("DEMO_DB_URL=jdbc:postgresql://127.0.0.1:$Port/beauty_center_demo", "DEMO_DB_PASSWORD=$databasePassword", "DEMO_JWT_SECRET=$jwt", 'DEMO_OWNER_USERNAME=portfolio_owner', "DEMO_OWNER_PASSWORD=$ownerPassword", "DEMO_STAFF_PASSWORD=$staffPassword")
[IO.File]::WriteAllLines($privateFile, $lines, [Text.UTF8Encoding]::new($false))
$identity = [Security.Principal.WindowsIdentity]::GetCurrent().Name
$acl = Get-Acl -LiteralPath $privateFile
$acl.SetAccessRuleProtection($true, $false)
$acl.SetAccessRule([Security.AccessControl.FileSystemAccessRule]::new($identity, 'FullControl', 'Allow'))
Set-Acl -LiteralPath $privateFile -AclObject $acl
"CREATE ROLE beauty_demo_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD '$databasePassword';" | docker exec -i $ContainerName sh -c 'psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres' | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Demo role creation failed; private configuration retained for diagnosis.' }
docker exec $ContainerName sh -c 'createdb -U "$POSTGRES_USER" --owner=beauty_demo_app --template=template0 beauty_center_demo'
if ($LASTEXITCODE -ne 0) { throw 'Demo database creation failed; nothing was removed or overwritten.' }
Write-Output 'Created beauty_center_demo with its own non-superuser role. Private credentials saved to ignored .env.demo.local; no client configuration was read or changed.'

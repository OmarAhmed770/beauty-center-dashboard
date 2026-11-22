# Example only. Secrets are read interactively and remain process-local.
$env:DB_URL = 'jdbc:postgresql://localhost:5432/beauty_center'
$env:DB_USERNAME = Read-Host 'PostgreSQL application username'
$databasePassword = Read-Host 'PostgreSQL application password' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $databasePassword).Password

$jwtBytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Fill($jwtBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$env:APP_TIMEZONE = 'Africa/Cairo'
$env:SERVER_ADDRESS = '127.0.0.1'
$env:SERVER_PORT = '8080'
$env:CORS_ALLOWED_ORIGINS = 'http://localhost:8088'
$env:SPRINGDOC_API_DOCS_ENABLED = 'false'
$env:SPRINGDOC_SWAGGER_UI_ENABLED = 'false'

$env:BACKUP_ENABLED = 'true'
$env:BACKUP_DIRECTORY = 'C:\BeautyCenterData\backups'
$env:BACKUP_RETENTION_COUNT = '30'
$env:PG_DUMP_PATH = 'C:\Program Files\PostgreSQL\17\bin\pg_dump.exe'
$env:BACKUP_TIMEOUT = '10m'
$env:BACKUP_FILENAME_PREFIX = 'beauty-center'

# The first OWNER is created through the one-time setup screen. Do not store an OWNER password here.

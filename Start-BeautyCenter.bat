
@echo off
setlocal EnableExtensions DisableDelayedExpansion

rem Local development launcher only. It never changes Docker volumes or database data.
set "PROJECT_ROOT=%~dp0"
pushd "%PROJECT_ROOT%" >nul || (echo ERROR: Could not open the project folder.& exit /b 1)

set "ENV_FILE=%CD%\.env"
set "LOCAL_SECRET_FILE=%CD%\.beautycenter-dev.env"
set "BACKEND_DIR=%CD%\backend"
set "FRONTEND_DIR=%CD%\frontend"
set "LOG_DIR=%TEMP%\BeautyCenterManagementSystem"
set "BACKEND_LOG=%LOG_DIR%\backend.log"
set "FRONTEND_LOG=%LOG_DIR%\frontend.log"

if not exist "%ENV_FILE%" (
  set "LAUNCH_ERROR=Missing .env. Copy .env.example to .env and set the local PostgreSQL values."
  goto :fatal
)

rem Load local values without echoing them. Docker Compose reads this file too.
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%ENV_FILE%") do set "%%A=%%B"

if not defined POSTGRES_DB (
  set "LAUNCH_ERROR=POSTGRES_DB is missing from .env."
  goto :fatal
)
if not defined POSTGRES_USER (
  set "LAUNCH_ERROR=POSTGRES_USER is missing from .env."
  goto :fatal
)
if not defined POSTGRES_PASSWORD (
  set "LAUNCH_ERROR=POSTGRES_PASSWORD is missing from .env."
  goto :fatal
)
if not defined POSTGRES_PORT set "POSTGRES_PORT=5432"
if not defined SERVER_ADDRESS set "SERVER_ADDRESS=127.0.0.1"
if not defined SERVER_PORT set "SERVER_PORT=8080"

rem Prefer a configured JWT secret. If none exists, persist one private development secret.
set "JWT_SECRET_CREATED="
if not defined JWT_SECRET if exist "%LOCAL_SECRET_FILE%" (
  for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%LOCAL_SECRET_FILE%") do set "%%A=%%B"
)
if not defined JWT_SECRET (
  for /f "usebackq delims=" %%S in (`powershell.exe -NoProfile -Command "$bytes = New-Object byte[] 48; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes); [Convert]::ToBase64String($bytes)"`) do set "JWT_SECRET=%%S"
  if not defined JWT_SECRET (
    set "LAUNCH_ERROR=Could not create a local development JWT secret."
    goto :fatal
  )
  set "JWT_SECRET_CREATED=1"
)
if defined JWT_SECRET_CREATED (
  setlocal EnableDelayedExpansion
  > "%LOCAL_SECRET_FILE%" echo JWT_SECRET=!JWT_SECRET!
  endlocal
)

if not defined DB_USERNAME set "DB_USERNAME=%POSTGRES_USER%"
if not defined DB_PASSWORD set "DB_PASSWORD=%POSTGRES_PASSWORD%"

where docker.exe >nul 2>&1 || (
  set "LAUNCH_ERROR=Docker CLI is unavailable. Start Docker Desktop and try again."
  goto :fatal
)
docker.exe info >nul 2>&1 || (
  set "LAUNCH_ERROR=Docker Desktop is not running or its engine is unavailable. Start Docker Desktop and wait for it to finish starting."
  goto :fatal
)
where java.exe >nul 2>&1 || (
  set "LAUNCH_ERROR=Java is unavailable on PATH. Install or select Java 17."
  goto :fatal
)
where mvn.cmd >nul 2>&1 || where mvn.exe >nul 2>&1 || (
  set "LAUNCH_ERROR=Maven is unavailable on PATH. Install Maven or add it to PATH."
  goto :fatal
)
where npm.cmd >nul 2>&1 || (
  set "LAUNCH_ERROR=npm.cmd is unavailable on PATH. Install Node.js 20 or newer."
  goto :fatal
)
where curl.exe >nul 2>&1 || (
  set "LAUNCH_ERROR=curl.exe is unavailable. Install the current Windows curl component and try again."
  goto :fatal
)

echo Starting PostgreSQL with Docker Compose...
docker.exe compose up -d postgres >nul 2>&1 || (
  set "LAUNCH_ERROR=Docker Compose could not start PostgreSQL. Review Docker Desktop and the local .env file."
  goto :fatal
)

setlocal EnableDelayedExpansion
set "PG_CONTAINER="
for /f "usebackq delims=" %%C in (`docker.exe compose ps -q postgres`) do set "PG_CONTAINER=%%C"
if not defined PG_CONTAINER (
  set "LAUNCH_ERROR=Docker Compose did not return a PostgreSQL container."
  goto :fatal
)
for /l %%A in (1,1,60) do (
  set "PG_STATUS="
  for /f "usebackq delims=" %%H in (`docker.exe inspect --format "{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}" !PG_CONTAINER! 2^>nul`) do set "PG_STATUS=%%H"
  if /i "!PG_STATUS!"=="healthy" goto :postgres_ready
  if /i "!PG_STATUS!"=="exited" goto :postgres_failed
  if /i "!PG_STATUS!"=="dead" goto :postgres_failed
  timeout /t 1 /nobreak >nul
)
goto :postgres_failed

:postgres_failed
set "LAUNCH_ERROR=PostgreSQL did not become healthy within 60 seconds."
goto :fatal

:postgres_ready

set "DOCKER_DB_PORT="
for /f "tokens=2 delims=:" %%P in ('docker.exe compose port postgres 5432') do set "DOCKER_DB_PORT=%%P"
if not defined DOCKER_DB_PORT set "DOCKER_DB_PORT=!POSTGRES_PORT!"
if not defined DB_URL set "DB_URL=jdbc:postgresql://127.0.0.1:!DOCKER_DB_PORT!/!POSTGRES_DB!"
set "BACKEND_URL=http://127.0.0.1:!SERVER_PORT!"
set "FRONTEND_URL=http://localhost:5173"
endlocal & set "DB_URL=%DB_URL%" & set "BACKEND_URL=%BACKEND_URL%" & set "FRONTEND_URL=%FRONTEND_URL%"

if not exist "%LOG_DIR%" md "%LOG_DIR%" >nul 2>&1

curl.exe --fail --silent --show-error --max-time 3 "%BACKEND_URL%/api/v1/setup/status" >nul 2>&1
if not errorlevel 1 goto :backend_ready
netstat -ano | findstr /r /c:":%SERVER_PORT% .*LISTENING" >nul 2>&1
if not errorlevel 1 (
  set "LAUNCH_ERROR=Port %SERVER_PORT% is occupied by a service that is not this backend."
  goto :fatal
)
echo Starting Spring Boot backend...
start "Beauty Center Backend" /min cmd.exe /d /c "cd /d ""%BACKEND_DIR%"" && mvn -q spring-boot:run > ""%BACKEND_LOG%"" 2>&1"
for /l %%A in (1,1,120) do (
  curl.exe --fail --silent --show-error --max-time 3 "%BACKEND_URL%/api/v1/setup/status" >nul 2>&1
  if not errorlevel 1 goto :backend_ready
  timeout /t 1 /nobreak >nul
)
set "LAUNCH_ERROR=Backend did not become ready. Review %BACKEND_LOG%."
goto :fatal

:backend_ready
echo Backend is ready at %BACKEND_URL%.

curl.exe --fail --silent --show-error --max-time 3 "%FRONTEND_URL%" >nul 2>&1
if not errorlevel 1 goto :frontend_ready
netstat -ano | findstr /r /c:":5173 .*LISTENING" >nul 2>&1
if not errorlevel 1 (
  set "LAUNCH_ERROR=Port 5173 is occupied by a service that is not the frontend."
  goto :fatal
)
echo Starting React frontend...
start "Beauty Center Frontend" /min cmd.exe /d /c "cd /d ""%FRONTEND_DIR%"" && npm.cmd run dev -- --host localhost --port 5173 > ""%FRONTEND_LOG%"" 2>&1"
for /l %%A in (1,1,90) do (
  curl.exe --fail --silent --show-error --max-time 3 "%FRONTEND_URL%" >nul 2>&1
  if not errorlevel 1 goto :frontend_ready
  timeout /t 1 /nobreak >nul
)
set "LAUNCH_ERROR=Frontend did not become ready. Review %FRONTEND_LOG%."
goto :fatal

:frontend_ready
echo Frontend is ready at %FRONTEND_URL%.

echo.
echo Beauty Center is ready at %FRONTEND_URL%
start "" "%FRONTEND_URL%"
popd
exit /b 0

:fatal
echo.
echo ERROR: %LAUNCH_ERROR%
if defined LOG_DIR echo Logs: %LOG_DIR%
popd
exit /b 1

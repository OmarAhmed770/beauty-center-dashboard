# Windows installation and startup

This guide covers the existing local application and its separate Demo. Commands below are instructions, not evidence of a fresh installation/build check. Keep any existing installation's database, private configuration, launcher and backups intact.

## 1. Prerequisites

- **Java 17 JDK** and Maven available to the terminal. Set `JAVA_HOME` to the installed JDK 17; do not rely on a different Java selected elsewhere on Windows.
- Node.js compatible with `frontend/package.json` (declared minimum **20.19.0**) and `npm.cmd`.
- PostgreSQL **17**: either native Windows PostgreSQL or Docker Desktop with the existing Compose service. pgAdmin is optional.
- PostgreSQL 17 client tools (`pg_dump.exe`, `pg_restore.exe`) accessible to the backend for backup/restore, even if the database itself runs in Docker.
- For `Start-BeautyCenter.bat`: Docker Desktop/Compose and Windows `curl.exe` are also required.

In a terminal, check `java -version`, `mvn -version`, `node --version` and `npm.cmd --version`. Maven must report Java 17. Initial package/image downloads need internet access; prepare dependencies before relying on offline operation.

## 2. Private configuration

For a **new** setup, copy `.env.example` to `.env` only if `.env` does not already exist. Edit privately; never replace an existing file with example values. Use simple `KEY=value` lines matching the sample. No usable credentials are included in this guide.

| Variable | Purpose |
| --- | --- |
| `DB_URL` | JDBC connection to the intended database, including host and actual port |
| `DB_USERNAME`, `DB_PASSWORD` | That database's application login; keep private |
| `JWT_SECRET` | A strong, independently generated secret of at least 32 characters; keep stable across normal launches |
| `JWT_ISSUER`, `JWT_ACCESS_TOKEN_EXPIRATION` | Token issuer and lifetime; application defaults are available |
| `SERVER_ADDRESS`, `SERVER_PORT` | Normally `127.0.0.1` and `8080` |
| `CORS_ALLOWED_ORIGINS` | Exact frontend origin(s), normally localhost/127.0.0.1 on port 5173 |
| `APP_TIMEZONE` | Business timezone; default `Africa/Cairo` |
| `VITE_API_BASE_URL` | Backend origin, without an extra `/api/v1` suffix |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT` | Docker Compose database initialization and published-port settings |

For example, the URL shape is `jdbc:postgresql://127.0.0.1:<port>/<database-name>`; replace the placeholders privately. Backend credentials must match the selected server, not a different local PostgreSQL instance.

Vite reads `frontend/.env.local` or inherited process variables. Direct Spring Boot/Maven startup does **not** automatically import the root `.env`; the supplied batch launcher handles its own environment loading. Never put passwords or JWT secrets in `VITE_*` variables: frontend values are public.

## 3. Normal startup with Docker

1. Start Docker Desktop and wait for its engine to be available.
2. Preserve the existing Compose volume. On an existing database, changing `POSTGRES_PASSWORD` in a file does not change the password stored in that database.
3. Confirm the actual published database port. Compose defaults to host port 5432. Keep `DB_URL` consistent.
4. Install frontend dependencies once from the project root:

   ```powershell
   Set-Location frontend
   npm.cmd ci
   Set-Location ..
   ```

5. Run `Start-BeautyCenter.bat` from the project root or by double-clicking it. It resolves its own location, starts the existing PostgreSQL service, waits for readiness, starts Maven `spring-boot:run` and Vite, then opens `http://localhost:5173`.

The launcher checks for already-running services. It does not install missing tools, provision an OWNER password, or provide a commercial Windows service/installer. Its logs are under `%TEMP%\BeautyCenterManagementSystem`; do not publish them without reviewing sensitive content.

## 4. Native PostgreSQL without Docker

For a new installation, use pgAdmin or your database administrator to create a dedicated login and database owned by that login. It needs schema creation/alteration rights for Flyway, not general superuser rights. For an existing installation, reuse its intended database and account—do not recreate them.

Set `DB_URL` to the native server's actual port (commonly 5432). Do not use the Docker-specific batch launcher. In each PowerShell terminal, from the project root, load only your trusted private configuration:

```powershell
foreach ($line in Get-Content -LiteralPath .env) {
    if ($line -match '^([A-Z][A-Z0-9_]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
    }
}
```

Then start the backend in one terminal:

```powershell
Set-Location backend
mvn spring-boot:run
```

Start the frontend in the other (install dependencies once as above):

```powershell
Set-Location frontend
npm.cmd run dev -- --host localhost --port 5173
```

Keep both processes running while using the application. Use Ctrl+C in their terminals to stop them normally. The database remains persistent.

## 5. First run and maintenance

- Flyway validates/applies V1–V17 on backend startup; JPA validates the schema. Check for `Started BeautyCenterApplication`. `/api/v1/setup/status` is the launcher's readiness endpoint.
- Open the frontend. A new empty installation offers OWNER setup; existing installations go to login. Complete setup on the local machine before giving others access. There is no shared/default login password.
- Configure the center name/currency and service prices. Optional starter services are inactive until priced/activated; they are not ready-to-sell zero-price services.
- In Settings > Backup, OWNER chooses an accessible private folder, retention and schedule. Configure `PG_DUMP_PATH`/`PG_RESTORE_PATH` if the tools are not on PATH. Backend backup defaults can also use `BACKUP_ENABLED`, `BACKUP_DIRECTORY` and `BACKUP_RETENTION_COUNT`; saved UI settings control the configured folder/schedule/retention.
- Keep a separate copy of important backups and verify restoration in a separate test database. Restore replaces the selected installation's data; never experiment on the working database.

## Portfolio demo

Use the live demo and accounts linked in the [README](../README.md#try-the-demo). The read-only profile blocks business writes and backup operations. Local installations use private configuration and first-run OWNER setup.

## Troubleshooting

| Symptom | Safe next step |
| --- | --- |
| `npm` fails in PowerShell | Try `npm.cmd`; check Node/npm PATH. Do not delete lockfiles or reinstall dependencies blindly. |
| Maven uses Java 23 or another version | Select JDK 17 with `JAVA_HOME` and the terminal PATH, then check `mvn -version` again. |
| Docker unavailable | Start Docker Desktop and wait, or use the documented native-PostgreSQL path instead. |
| Database password authentication fails | Check the intended host/port and credential source privately. Do not erase the volume or assume editing `.env` changes an existing database password. |
| Flyway checksum mismatch | Stop and investigate the applied migration version. Do not edit old migrations, run repair blindly, or reset data. |
| Port already occupied | Identify the listening process; reuse the correct service or stop only its known process. Do not terminate unrelated applications. |
| Frontend cannot reach backend | Check backend readiness, `VITE_API_BASE_URL`, port and exact CORS origin. Restart Vite after environment changes. |
| Login refused | Check pending approval, active status, credentials and temporary lockout. OWNER manages accounts; no default password exists. |
| Daily report has no saved snapshot | Choose a previously closed date; use a date-range report for live totals. Demo must not create a closing to hide the message. |
| Backup/restore unavailable | OWNER access, running PostgreSQL client tools, a writable folder and sufficient disk space are required. Demo blocks these actions deliberately. |

This source distribution is not an automatic offline installer. First-run downloads, printer setup and machine-specific service configuration must be prepared by the installer; do not expose development servers publicly.


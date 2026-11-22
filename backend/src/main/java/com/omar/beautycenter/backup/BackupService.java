package com.omar.beautycenter.backup;

import com.omar.beautycenter.audit.AuditLogService;
import com.omar.beautycenter.backup.DatabaseBackupExecutor.BackupCommand;
import com.omar.beautycenter.backup.dto.BackupConfigurationRequest;
import com.omar.beautycenter.backup.dto.BackupConfigurationResponse;
import com.omar.beautycenter.backup.dto.BackupHistoryResponse;
import com.omar.beautycenter.backup.dto.RestoreBackupRequest;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.common.error.NotFoundException;
import com.omar.beautycenter.role.Role;
import com.omar.beautycenter.user.User;
import com.omar.beautycenter.user.UserRepository;
import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class BackupService {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private final BackupProperties properties;
    private final DataSourceProperties dataSourceProperties;
    private final DatabaseBackupExecutor backupExecutor;
    private final DatabaseRestoreExecutor restoreExecutor;
    private final PostgresToolResolver toolResolver;
    private final BackupSettingsRepository settingsRepository;
    private final DatabaseBackupRepository backupRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final DataSource dataSource;
    private final MaintenanceMode maintenanceMode;
    private final Clock clock;
    private final AtomicBoolean running = new AtomicBoolean();

    public BackupService(BackupProperties properties, DataSourceProperties dataSourceProperties,
            DatabaseBackupExecutor backupExecutor, DatabaseRestoreExecutor restoreExecutor,
            PostgresToolResolver toolResolver, BackupSettingsRepository settingsRepository,
            DatabaseBackupRepository backupRepository, UserRepository userRepository,
            PasswordEncoder passwordEncoder, AuditLogService auditLogService, DataSource dataSource,
            MaintenanceMode maintenanceMode, Clock clock) {
        this.properties = properties;
        this.dataSourceProperties = dataSourceProperties;
        this.backupExecutor = backupExecutor;
        this.restoreExecutor = restoreExecutor;
        this.toolResolver = toolResolver;
        this.settingsRepository = settingsRepository;
        this.backupRepository = backupRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.dataSource = dataSource;
        this.maintenanceMode = maintenanceMode;
        this.clock = clock;
    }

    @PreAuthorize("hasRole('OWNER')")
    public BackupStatusResponse status() {
        BackupSettings settings = settings();
        PostgresToolResolver.ToolResolution tools = tools();
        List<DatabaseBackup> history = backupRepository.findAllByOrderByStartedAtDesc();
        DatabaseBackup latest = history.stream().findFirst().orElse(null);
        DatabaseBackup successful = history.stream().filter(item -> item.getStatus() == BackupRunStatus.SUCCESS).findFirst().orElse(null);
        BackupRunStatus status = !properties.enabled()
                ? BackupRunStatus.DISABLED
                : running.get() ? BackupRunStatus.RUNNING : latest == null ? BackupRunStatus.NEVER_RUN : latest.getStatus();
        return new BackupStatusResponse(properties.enabled(), running.get(), tools.pgDumpPath() != null,
                tools.pgRestorePath() != null, status, latest == null ? null : latest.getStartedAt(),
                successful == null ? null : successful.getCompletedAt(), successful == null ? null : successful.getFileName(),
                successful == null ? null : successful.getFileSizeBytes(), successful == null ? null : successful.getSha256(),
                settings.getBackupDirectory(), settings.getRetentionCount(), statusMessage(tools, latest));
    }

    @PreAuthorize("hasRole('OWNER')")
    public BackupConfigurationResponse configuration() { return BackupConfigurationResponse.from(settings()); }

    @PreAuthorize("hasRole('OWNER')")
    public BackupConfigurationResponse updateConfiguration(BackupConfigurationRequest request) {
        Path directory = validatedDirectory(request.backupDirectory());
        BackupSettings settings = settings();
        settings.update(directory.toString(), request.automaticEnabled(), request.automaticTime(), request.retentionCount(), actor(), LocalDateTime.now(clock));
        settingsRepository.saveAndFlush(settings);
        auditLogService.record("DATABASE_BACKUP_SETTINGS_UPDATED", "BACKUP_SETTINGS", BackupSettings.SINGLETON_ID, null,
                Map.of("automaticEnabled", request.automaticEnabled(), "automaticTime", request.automaticTime(), "retentionCount", request.retentionCount()));
        return BackupConfigurationResponse.from(settings);
    }

    @PreAuthorize("hasRole('OWNER')")
    public List<BackupHistoryResponse> history() { return backupRepository.findAllByOrderByStartedAtDesc().stream().map(BackupHistoryResponse::from).toList(); }

    @PreAuthorize("hasRole('OWNER')")
    public BackupStatusResponse runManualBackup() { runBackup(BackupType.MANUAL, actor()); return status(); }

    /** Invoked only by the local scheduler; it has no web route. */
    public void runAutomaticBackupIfDue() {
        if (!properties.enabled() || running.get()) return;
        BackupSettings settings = settings();
        LocalDateTime now = LocalDateTime.now(clock);
        if (!settings.isAutomaticEnabled() || now.getHour() != settings.getAutomaticTime().getHour() || now.getMinute() != settings.getAutomaticTime().getMinute()) return;
        LocalDateTime start = now.toLocalDate().atStartOfDay();
        if (backupRepository.existsByBackupTypeAndStatusAndStartedAtBetween(BackupType.AUTOMATIC, BackupRunStatus.SUCCESS, start, start.plusDays(1))) return;
        try { runBackup(BackupType.AUTOMATIC, "SYSTEM"); } catch (BusinessRuleException ignored) { }
    }

    @PreAuthorize("hasRole('OWNER')")
    public void deleteBackup(Long backupId) {
        DatabaseBackup backup = successfulBackup(backupId);
        try {
            Files.deleteIfExists(backupPath(backup));
            backupRepository.delete(backup);
            auditLogService.record("DATABASE_BACKUP_DELETED", "DATABASE_BACKUP", backupId, null, Map.of("fileName", backup.getFileName()));
        } catch (IOException exception) { throw new BusinessRuleException("Could not delete the selected backup file."); }
    }

    @PreAuthorize("hasRole('OWNER')")
    public void openFolder() {
        try {
            Path directory = validatedDirectory(settings().getBackupDirectory());
            if (!Desktop.isDesktopSupported()) throw new IOException("Desktop integration unavailable.");
            Desktop.getDesktop().open(directory.toFile());
        } catch (Exception exception) { throw new BusinessRuleException("Could not open the backup folder. Open it manually from the shown path."); }
    }

    @PreAuthorize("hasRole('OWNER')")
    public BackupStatusResponse restore(Long backupId, RestoreBackupRequest request) {
        DatabaseBackup backup = successfulBackup(backupId);
        if (!request.confirmation().strip().equals("استعادة " + backup.getFileName())) throw new BusinessRuleException("Restore confirmation text does not match the backup file.");
        assertCurrentOwnerPassword(request.ownerPassword());
        Path file = backupPath(backup);
        verifyChecksum(backup, file);
        PostgresToolResolver.ToolResolution tools = tools();
        if (tools.pgRestorePath() == null) throw new BusinessRuleException("pg_restore was not found. Install PostgreSQL command-line tools.");
        try { restoreExecutor.validate(tools.pgRestorePath(), file, properties.timeout()); }
        catch (Exception exception) { throw new BusinessRuleException("The selected backup file could not be validated."); }
        runBackup(BackupType.SAFETY, actor());
        try {
            maintenanceMode.runWithWritesBlocked(() -> {
                restoreExecutor.restore(new DatabaseRestoreExecutor.RestoreCommand(tools.pgRestorePath(), dataSourceProperties.determineUrl(),
                        dataSourceProperties.determineUsername(), dataSourceProperties.determinePassword(), file, properties.timeout()));
                verifyDatabaseConnectivity();
            });
            auditLogService.record("DATABASE_RESTORE_COMPLETED", "DATABASE_BACKUP", backupId, null, Map.of("fileName", backup.getFileName()));
            return status();
        } catch (Exception exception) { throw new BusinessRuleException("Restore failed. The safety backup was kept; check the local server log."); }
    }

    private void runBackup(BackupType type, String initiatedBy) {
        if (!properties.enabled()) throw new BusinessRuleException("Database backup is disabled.");
        if (!running.compareAndSet(false, true)) throw new BusinessRuleException("A database backup is already running.");
        Path temporary = null;
        DatabaseBackup record = null;
        try {
            PostgresToolResolver.ToolResolution tools = tools();
            if (tools.pgDumpPath() == null) throw new BusinessRuleException("pg_dump was not found. Install PostgreSQL command-line tools.");
            BackupSettings settings = settings();
            Path directory = validatedDirectory(settings.getBackupDirectory());
            String fileName = uniqueFileName(directory, type, LocalDateTime.now(clock));
            record = backupRepository.saveAndFlush(new DatabaseBackup(fileName, type, LocalDateTime.now(clock), initiatedBy));
            temporary = Files.createTempFile(directory, "." + fileName + "-", ".tmp");
            backupExecutor.execute(new BackupCommand(tools.pgDumpPath(), dataSourceProperties.determineUrl(), dataSourceProperties.determineUsername(), dataSourceProperties.determinePassword(), temporary, properties.timeout()));
            long size = Files.size(temporary);
            if (size <= 0) throw new IOException("pg_dump created an empty backup file.");
            Path output = directory.resolve(fileName);
            moveCompleted(temporary, output);
            temporary = null;
            String checksum = sha256(output);
            record.succeed(size, checksum, LocalDateTime.now(clock));
            backupRepository.saveAndFlush(record);
            // A restore safety backup must not prune either the selected source or its recovery copy.
            if (type != BackupType.SAFETY) enforceRetention(directory, settings.getRetentionCount());
            auditLogService.record("DATABASE_BACKUP_COMPLETED", "DATABASE_BACKUP", record.getId(), null, Map.of("fileName", fileName, "sizeBytes", size, "sha256", checksum, "type", type));
        } catch (Exception exception) {
            if (record != null) { record.fail("فشل إنشاء النسخة الاحتياطية. راجع أدوات PostgreSQL ومسار المجلد.", LocalDateTime.now(clock)); backupRepository.saveAndFlush(record); }
            throw new BusinessRuleException("Could not create a local PostgreSQL backup. Ensure pg_dump is installed and accessible.");
        } finally {
            running.set(false);
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }

    private BackupSettings settings() {
        return settingsRepository.findById(BackupSettings.SINGLETON_ID).orElseGet(() -> settingsRepository.saveAndFlush(new BackupSettings(
                validatedDirectory(properties.directory()).toString(), false, LocalTime.of(2, 0), properties.retentionCount(), "SYSTEM", LocalDateTime.now(clock))));
    }
    private DatabaseBackup successfulBackup(Long id) {
        DatabaseBackup backup = backupRepository.findById(id).orElseThrow(() -> new NotFoundException("Backup not found."));
        if (backup.getStatus() != BackupRunStatus.SUCCESS) throw new BusinessRuleException("Only a successful backup can be used.");
        return backup;
    }
    private Path backupPath(DatabaseBackup backup) {
        Path directory = validatedDirectory(settings().getBackupDirectory());
        Path file = directory.resolve(backup.getFileName()).normalize();
        if (!file.startsWith(directory) || !Files.isRegularFile(file)) throw new BusinessRuleException("The selected backup file is no longer available.");
        return file;
    }
    private void enforceRetention(Path directory, int retention) {
        List<DatabaseBackup> successful = backupRepository.findAllByOrderByStartedAtDesc().stream().filter(item -> item.getStatus() == BackupRunStatus.SUCCESS).toList();
        for (int index = retention; index < successful.size(); index++) {
            DatabaseBackup old = successful.get(index);
            try { Files.deleteIfExists(directory.resolve(old.getFileName()).normalize()); backupRepository.delete(old); } catch (IOException ignored) { }
        }
    }
    private String uniqueFileName(Path directory, BackupType type, LocalDateTime now) {
        String base = properties.filenamePrefix() + "-" + type.name().toLowerCase() + "-" + FILE_TIME.format(now);
        java.util.Set<String> recorded = backupRepository.findAllByOrderByStartedAtDesc().stream()
                .map(DatabaseBackup::getFileName).collect(java.util.stream.Collectors.toSet());
        String candidate = base + ".dump";
        for (int index = 1; Files.exists(directory.resolve(candidate)) || recorded.contains(candidate); index++) {
            candidate = base + "-" + index + ".dump";
        }
        return candidate;
    }
    private void assertCurrentOwnerPassword(String rawPassword) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User owner = authentication == null ? null : userRepository.findByUsername(authentication.getName()).orElse(null);
        if (owner == null || !owner.getRoles().contains(Role.OWNER) || !passwordEncoder.matches(rawPassword, owner.getPasswordHash())) throw new BusinessRuleException("Owner authorization could not be verified.");
    }
    private void verifyDatabaseConnectivity() throws Exception { try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("SELECT 1")) { statement.execute(); } }
    private void verifyChecksum(DatabaseBackup backup, Path file) { try { if (!sha256(file).equals(backup.getSha256())) throw new BusinessRuleException("Backup checksum does not match the stored record."); } catch (IOException exception) { throw new BusinessRuleException("Could not verify the selected backup file."); } }
    private PostgresToolResolver.ToolResolution tools() { return toolResolver.resolve(properties.pgDumpPath(), properties.pgRestorePath()); }
    private String actor() { Authentication auth = SecurityContextHolder.getContext().getAuthentication(); return auth == null ? "SYSTEM" : auth.getName(); }
    private static Path validatedDirectory(String directory) { try { Path path = Path.of(directory).toAbsolutePath().normalize(); Files.createDirectories(path); return path; } catch (Exception exception) { throw new BusinessRuleException("The backup folder is not available or cannot be created."); } }
    private static void moveCompleted(Path source, Path target) throws IOException { try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE); } catch (AtomicMoveNotSupportedException exception) { Files.move(source, target); } }
    private static String sha256(Path path) throws IOException { try { MessageDigest digest = MessageDigest.getInstance("SHA-256"); try (InputStream input = Files.newInputStream(path)) { byte[] buffer = new byte[8192]; for (int read; (read = input.read(buffer)) >= 0;) digest.update(buffer, 0, read); } return HexFormat.of().formatHex(digest.digest()); } catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 is unavailable.", exception); } }
    private static String statusMessage(PostgresToolResolver.ToolResolution tools, DatabaseBackup latest) { if (tools.pgDumpPath() == null) return "أداة pg_dump غير موجودة. ثبّت أدوات PostgreSQL المحلية."; if (tools.pgRestorePath() == null) return "أداة pg_restore غير موجودة. ثبّت أدوات PostgreSQL المحلية."; if (latest == null) return "جاهز لإنشاء أول نسخة احتياطية محلية."; return latest.getStatus() == BackupRunStatus.SUCCESS ? "آخر نسخة احتياطية متاحة." : "فشلت آخر محاولة نسخ احتياطي."; }
}

// TEST ONLY - credential literals in this file are synthetic test fixtures.
package com.omar.beautycenter.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.omar.beautycenter.audit.AuditLogService;
import com.omar.beautycenter.backup.dto.RestoreBackupRequest;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.role.Role;
import com.omar.beautycenter.user.User;
import com.omar.beautycenter.user.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Set;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class BackupServiceTest {

    @TempDir Path temporaryDirectory;

    @Test
    void createsVerifiedCustomBackupWithChecksumAndRecordedHistory() throws Exception {
        DatabaseBackupRepository backups = recordingRepository();
        BackupService service = service(backups, command -> Files.writeString(command.outputFile(), "valid backup bytes"));

        BackupStatusResponse response = service.runManualBackup();

        assertThat(response.lastAttemptStatus()).isEqualTo(BackupRunStatus.SUCCESS);
        assertThat(response.lastFileName()).endsWith(".dump");
        assertThat(response.lastFileSizeBytes()).isPositive();
        assertThat(response.lastFileSha256()).hasSize(64);
        assertThat(Files.list(temporaryDirectory).filter(path -> path.toString().endsWith(".dump")).count()).isEqualTo(1);
    }

    @Test
    void reportsMissingToolWithoutPretendingBackupSucceeded() {
        DatabaseBackupRepository backups = mock(DatabaseBackupRepository.class);
        when(backups.findAllByOrderByStartedAtDesc()).thenReturn(List.of());
        PostgresToolResolver tools = mock(PostgresToolResolver.class);
        when(tools.resolve(any(), any())).thenReturn(new PostgresToolResolver.ToolResolution(null, null, false));
        BackupService service = service(backups, command -> { throw new AssertionError("executor must not run"); }, tools);

        BackupStatusResponse response = service.status();

        assertThat(response.pgDumpAvailable()).isFalse();
        assertThat(response.pgRestoreAvailable()).isFalse();
        assertThat(response.message()).contains("pg_dump");
    }

    @Test
    void restoringAnOldBackupDoesNotDeleteItWhenSafetyBackupExceedsRetention() throws Exception {
        verifyRestorePreservesRecoveryFiles(false);
    }

    @Test
    void failedRestoreKeepsBothSelectedBackupAndSafetyCopy() throws Exception {
        verifyRestorePreservesRecoveryFiles(true);
    }

    private void verifyRestorePreservesRecoveryFiles(boolean failRestore) throws Exception {
        Path original = temporaryDirectory.resolve("selected-old.dump");
        Files.writeString(original, "original backup test bytes");
        DatabaseBackup selected = new DatabaseBackup(original.getFileName().toString(), BackupType.MANUAL, now().minusDays(1), "owner");
        selected.succeed(Files.size(original), HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(original))), now().minusDays(1));
        List<DatabaseBackup> records = new ArrayList<>(List.of(selected));
        DatabaseBackupRepository backups = mock(DatabaseBackupRepository.class);
        when(backups.findById(1L)).thenReturn(Optional.of(selected));
        when(backups.findAllByOrderByStartedAtDesc()).thenAnswer(call -> records.stream()
                .sorted(java.util.Comparator.comparing(DatabaseBackup::getStartedAt).reversed()).toList());
        when(backups.saveAndFlush(any(DatabaseBackup.class))).thenAnswer(call -> {
            DatabaseBackup record = call.getArgument(0);
            if (!records.contains(record)) records.add(record);
            return record;
        });
        BackupSettingsRepository settings = mock(BackupSettingsRepository.class);
        when(settings.findById(anyLong())).thenReturn(Optional.of(new BackupSettings(temporaryDirectory.toString(), false, LocalTime.of(2, 0), 1, "SYSTEM", now())));
        PostgresToolResolver tools = mock(PostgresToolResolver.class);
        when(tools.resolve(any(), any())).thenReturn(new PostgresToolResolver.ToolResolution("pg_dump", "pg_restore", true));
        User owner = mock(User.class);
        when(owner.getRoles()).thenReturn(Set.of(Role.OWNER));
        when(owner.getPasswordHash()).thenReturn("test-only-hash");
        UserRepository users = mock(UserRepository.class);
        when(users.findByUsername("owner")).thenReturn(Optional.of(owner));
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        when(passwords.matches("test-only-password", "test-only-hash")).thenReturn(true);
        DatabaseRestoreExecutor restore = mock(DatabaseRestoreExecutor.class);
        MaintenanceMode maintenance = new MaintenanceMode();
        doAnswer(call -> {
            assertThat(original).exists();
            assertThat(maintenance.isWriteBlocked()).isTrue();
            if (failRestore) throw new IOException("simulated restore error");
            return null;
        }).when(restore).restore(any());
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT 1")).thenReturn(mock(PreparedStatement.class));
        DataSourceProperties source = new DataSourceProperties();
        source.setUrl("jdbc:postgresql://localhost/isolated_test");
        source.setUsername("test-only-user");
        source.setPassword("test-only-password");
        BackupService service = new BackupService(new BackupProperties(true, temporaryDirectory.toString(), 1, "pg_dump", "pg_restore", Duration.ofMinutes(1), "beauty-test"),
                source, command -> Files.writeString(command.outputFile(), "safety backup test bytes"), restore, tools,
                settings, backups, users, passwords, mock(AuditLogService.class), dataSource, maintenance, fixedClock());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("owner", "unused"));
        try {
            RestoreBackupRequest request = new RestoreBackupRequest("test-only-password", "استعادة " + selected.getFileName());
            if (failRestore) assertThatThrownBy(() -> service.restore(1L, request)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("safety backup was kept");
            else service.restore(1L, request);
        } finally {
            SecurityContextHolder.clearContext();
        }
        verify(restore).restore(any());
        verify(backups, never()).delete(any());
        assertThat(Files.readString(original)).isEqualTo("original backup test bytes");
        assertThat(records).hasSize(2);
        DatabaseBackup safety = records.stream().filter(record -> record.getBackupType() == BackupType.SAFETY).findFirst().orElseThrow();
        assertThat(safety.getStatus()).isEqualTo(BackupRunStatus.SUCCESS);
        assertThat(temporaryDirectory.resolve(safety.getFileName())).exists();
        assertThat(maintenance.isWriteBlocked()).isFalse();
    }

    private BackupService service(DatabaseBackupRepository backups, DatabaseBackupExecutor executor) {
        PostgresToolResolver tools = mock(PostgresToolResolver.class);
        when(tools.resolve(any(), any())).thenReturn(new PostgresToolResolver.ToolResolution("pg_dump", "pg_restore", true));
        return service(backups, executor, tools);
    }

    private static DatabaseBackupRepository recordingRepository() {
        List<DatabaseBackup> records = new ArrayList<>();
        DatabaseBackupRepository repository = mock(DatabaseBackupRepository.class);
        when(repository.findAllByOrderByStartedAtDesc()).thenAnswer(call -> List.copyOf(records));
        when(repository.saveAndFlush(any(DatabaseBackup.class))).thenAnswer(call -> {
            DatabaseBackup backup = call.getArgument(0);
            if (!records.contains(backup)) records.add(backup);
            return backup;
        });
        return repository;
    }

    private BackupService service(DatabaseBackupRepository backups, DatabaseBackupExecutor executor, PostgresToolResolver tools) {
        BackupSettings settings = new BackupSettings(temporaryDirectory.toString(), false, LocalTime.of(2, 0), 30, "SYSTEM", now());
        BackupSettingsRepository settingsRepository = mock(BackupSettingsRepository.class);
        when(settingsRepository.findById(anyLong())).thenReturn(Optional.of(settings));
        when(settingsRepository.saveAndFlush(any(BackupSettings.class))).thenAnswer(call -> call.getArgument(0));
        DataSourceProperties source = new DataSourceProperties();
        source.setUrl("jdbc:postgresql://localhost:5432/beauty_center");
        source.setUsername("beauty_app");
        source.setPassword("test-secret");
        BackupProperties properties = new BackupProperties(true, temporaryDirectory.toString(), 30, "pg_dump", "pg_restore", Duration.ofMinutes(1), "beauty-test");
        return new BackupService(properties, source, executor, mock(DatabaseRestoreExecutor.class), tools,
                settingsRepository, backups, mock(UserRepository.class), mock(PasswordEncoder.class), mock(AuditLogService.class),
                mock(DataSource.class), new MaintenanceMode(), fixedClock());
    }

    private static Clock fixedClock() { return Clock.fixed(Instant.parse("2026-11-14T09:00:00Z"), ZoneId.of("Africa/Cairo")); }
    private static LocalDateTime now() { return LocalDateTime.now(fixedClock()); }
}

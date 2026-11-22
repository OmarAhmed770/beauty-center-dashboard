package com.omar.beautycenter.backup;

import java.time.LocalDateTime;

public record BackupStatusResponse(
        boolean enabled,
        boolean inProgress,
        boolean pgDumpAvailable,
        boolean pgRestoreAvailable,
        BackupRunStatus lastAttemptStatus,
        LocalDateTime lastAttemptAt,
        LocalDateTime lastSuccessfulAt,
        String lastFileName,
        Long lastFileSizeBytes,
        String lastFileSha256,
        String backupDirectory,
        int retentionCount,
        String message) {
}

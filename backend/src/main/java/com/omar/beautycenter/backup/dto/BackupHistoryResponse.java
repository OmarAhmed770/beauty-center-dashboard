package com.omar.beautycenter.backup.dto;

import com.omar.beautycenter.backup.DatabaseBackup;
import com.omar.beautycenter.backup.BackupRunStatus;
import com.omar.beautycenter.backup.BackupType;
import java.time.LocalDateTime;

public record BackupHistoryResponse(
        Long id,
        String fileName,
        BackupType backupType,
        BackupRunStatus status,
        Long fileSizeBytes,
        String sha256,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String errorMessage) {
    public static BackupHistoryResponse from(DatabaseBackup backup) {
        return new BackupHistoryResponse(backup.getId(), backup.getFileName(), backup.getBackupType(), backup.getStatus(),
                backup.getFileSizeBytes(), backup.getSha256(), backup.getStartedAt(), backup.getCompletedAt(), backup.getErrorMessage());
    }
}

package com.omar.beautycenter.backup.dto;

import com.omar.beautycenter.backup.BackupSettings;
import java.time.LocalTime;

public record BackupConfigurationResponse(
        String backupDirectory,
        boolean automaticEnabled,
        LocalTime automaticTime,
        int retentionCount) {
    public static BackupConfigurationResponse from(BackupSettings settings) {
        return new BackupConfigurationResponse(settings.getBackupDirectory(), settings.isAutomaticEnabled(),
                settings.getAutomaticTime(), settings.getRetentionCount());
    }
}

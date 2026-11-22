package com.omar.beautycenter.backup;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BackupScheduler {
    private final BackupService backupService;
    public BackupScheduler(BackupService backupService) { this.backupService = backupService; }
    @Scheduled(cron = "0 * * * * *")
    public void runDailyBackupWhenDue() { backupService.runAutomaticBackupIfDue(); }
}

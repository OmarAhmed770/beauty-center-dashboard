package com.omar.beautycenter.backup;

import java.nio.file.Path;
import java.time.Duration;

@FunctionalInterface
public interface DatabaseBackupExecutor {

    void execute(BackupCommand command) throws Exception;

    record BackupCommand(
            String executable,
            String databaseUrl,
            String username,
            String password,
            Path outputFile,
            Duration timeout) {
    }
}

package com.omar.beautycenter.backup;

import java.nio.file.Path;
import java.time.Duration;

public interface DatabaseRestoreExecutor {
    void validate(String executable, Path backupFile, Duration timeout) throws Exception;
    void restore(RestoreCommand command) throws Exception;

    record RestoreCommand(String executable, String databaseUrl, String username, String password,
                          Path backupFile, Duration timeout) { }
}

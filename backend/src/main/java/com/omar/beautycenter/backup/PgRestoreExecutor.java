package com.omar.beautycenter.backup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class PgRestoreExecutor implements DatabaseRestoreExecutor {

    @Override
    public void validate(String executable, Path backupFile, Duration timeout) throws Exception {
        run(List.of(executable, "--list", "--no-password", backupFile.toString()), null, timeout, backupFile.getParent());
    }

    @Override
    public void restore(RestoreCommand command) throws Exception {
        String connection = postgresConnection(command.databaseUrl());
        run(List.of(command.executable(), "--single-transaction", "--exit-on-error", "--clean", "--if-exists", "--no-owner", "--no-privileges", "--no-password",
                "--dbname=" + connection, "--username=" + command.username(), command.backupFile().toString()),
                command.password(), command.timeout(), command.backupFile().getParent());
    }

    private static void run(List<String> command, String password, Duration timeout, Path workingDirectory) throws Exception {
        Path diagnostic = Files.createTempFile(workingDirectory, ".pg-restore-", ".log");
        try {
            ProcessBuilder builder = new ProcessBuilder(new ArrayList<>(command));
            builder.redirectErrorStream(true);
            builder.redirectOutput(diagnostic.toFile());
            if (password != null && !password.isBlank()) builder.environment().put("PGPASSWORD", password);
            Process process = builder.start();
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IOException("pg_restore exceeded the configured timeout.");
            }
            if (process.exitValue() != 0) throw new IOException("pg_restore failed: " + diagnosticText(diagnostic));
        } finally {
            Files.deleteIfExists(diagnostic);
        }
    }

    private static String postgresConnection(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("Restore requires a PostgreSQL JDBC URL.");
        }
        return jdbcUrl.substring("jdbc:".length());
    }

    private static String diagnosticText(Path file) throws IOException {
        String text = Files.readString(file, StandardCharsets.UTF_8).strip();
        return text.length() <= 1000 ? text : text.substring(0, 1000);
    }
}

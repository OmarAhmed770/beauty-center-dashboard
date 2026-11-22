package com.omar.beautycenter.backup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class PgDumpBackupExecutor implements DatabaseBackupExecutor {

    @Override
    public void execute(BackupCommand command) throws Exception {
        String connection = postgresConnection(command.databaseUrl());
        Path diagnostic = Files.createTempFile(command.outputFile().getParent(), ".pg-dump-", ".log");
        try {
            ProcessBuilder builder = new ProcessBuilder(List.of(
                    command.executable(),
                    "--format=custom",
                    "--no-owner",
                    "--no-privileges",
                    "--no-password",
                    "--file=" + command.outputFile(),
                    "--dbname=" + connection,
                    "--username=" + command.username()));
            builder.redirectErrorStream(true);
            builder.redirectOutput(diagnostic.toFile());
            if (command.password() != null && !command.password().isBlank()) {
                builder.environment().put("PGPASSWORD", command.password());
            }
            Process process = builder.start();
            boolean finished;
            try {
                finished = process.waitFor(command.timeout().toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
                throw new IOException("PostgreSQL backup was interrupted.", exception);
            }
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("pg_dump exceeded the configured timeout.");
            }
            if (process.exitValue() != 0) {
                throw new IOException("pg_dump exited with code " + process.exitValue() + ": "
                        + diagnosticText(diagnostic));
            }
        } finally {
            Files.deleteIfExists(diagnostic);
        }
    }

    private static String postgresConnection(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("Backup requires a PostgreSQL JDBC URL.");
        }
        return jdbcUrl.substring("jdbc:".length());
    }

    private static String diagnosticText(Path file) throws IOException {
        String text = Files.readString(file, StandardCharsets.UTF_8).strip();
        return text.length() <= 1000 ? text : text.substring(0, 1000);
    }
}

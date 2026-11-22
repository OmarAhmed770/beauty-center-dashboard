package com.omar.beautycenter.backup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class PostgresToolResolver {

    public ToolResolution resolve(String configuredDump, String configuredRestore) {
        String dump = resolveTool(configuredDump, "pg_dump");
        String restore = resolveTool(configuredRestore, "pg_restore");
        return new ToolResolution(dump, restore, dump != null && restore != null);
    }

    private String resolveTool(String configured, String baseName) {
        if (configured != null && !configured.isBlank() && executable(configured.strip())) return configured.strip();
        String discovered = executable(baseName) ? baseName : null;
        if (discovered != null) return discovered;
        Path root = Path.of(System.getenv().getOrDefault("ProgramFiles", "C:\\Program Files"), "PostgreSQL");
        if (!Files.isDirectory(root)) return null;
        try (var versions = Files.list(root)) {
            return versions.filter(Files::isDirectory)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), Comparator.reverseOrder()))
                    .map(version -> version.resolve("bin").resolve(baseName + ".exe"))
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .findFirst().orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean executable(String command) {
        try {
            Process process = new ProcessBuilder(List.of(command, "--version")).redirectErrorStream(true).start();
            boolean completed = process.waitFor(Duration.ofSeconds(2).toMillis(), TimeUnit.MILLISECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    public record ToolResolution(String pgDumpPath, String pgRestorePath, boolean available) { }
}

// TEST ONLY - credential literals in this file are synthetic test fixtures.
package com.omar.beautycenter.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PgRestoreExecutorTest {
    @TempDir Path directory;

    @Test
    void restoreUsesOneTransactionAndStopsOnErrorsWithoutPuttingPasswordOnCommandLine() throws Exception {
        Process process = mock(Process.class);
        when(process.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(process.exitValue()).thenReturn(0);
        List<String> arguments = new ArrayList<>();
        var environment = new HashMap<String, String>();
        try (var construction = mockConstruction(ProcessBuilder.class, (builder, context) -> {
            for (Object argument : (List<?>) context.arguments().get(0)) arguments.add((String) argument);
            when(builder.redirectErrorStream(true)).thenReturn(builder);
            when(builder.redirectOutput(any(File.class))).thenReturn(builder);
            when(builder.environment()).thenReturn(environment);
            when(builder.start()).thenReturn(process);
        })) {
            new PgRestoreExecutor().restore(new DatabaseRestoreExecutor.RestoreCommand("pg_restore",
                    "jdbc:postgresql://localhost/isolated_test", "test-user", "test-only-password",
                    directory.resolve("test.dump"), Duration.ofSeconds(5)));
            assertThat(construction.constructed()).hasSize(1);
        }
        assertThat(arguments).contains("--single-transaction", "--exit-on-error", "--clean", "--if-exists", "--no-password");
        assertThat(arguments).noneMatch(argument -> argument.contains("test-only-password"));
        assertThat(environment).containsEntry("PGPASSWORD", "test-only-password");
    }
}

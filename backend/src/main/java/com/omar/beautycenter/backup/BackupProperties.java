package com.omar.beautycenter.backup;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.backup")
public record BackupProperties(
        boolean enabled,
        @NotBlank String directory,
        @Min(1) @Max(365) int retentionCount,
        @NotBlank String pgDumpPath,
        @NotBlank String pgRestorePath,
        @NotNull Duration timeout,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,50}") String filenamePrefix) {
}

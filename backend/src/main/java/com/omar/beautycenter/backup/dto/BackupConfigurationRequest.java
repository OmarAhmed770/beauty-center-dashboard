package com.omar.beautycenter.backup.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record BackupConfigurationRequest(
        @NotBlank String backupDirectory,
        boolean automaticEnabled,
        @NotNull LocalTime automaticTime,
        @Min(1) @Max(365) int retentionCount) { }

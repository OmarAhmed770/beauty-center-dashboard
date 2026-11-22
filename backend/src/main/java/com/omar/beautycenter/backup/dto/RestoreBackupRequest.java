package com.omar.beautycenter.backup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestoreBackupRequest(
        @NotBlank @Size(max = 200) String ownerPassword,
        @NotBlank @Size(max = 600) String confirmation) { }

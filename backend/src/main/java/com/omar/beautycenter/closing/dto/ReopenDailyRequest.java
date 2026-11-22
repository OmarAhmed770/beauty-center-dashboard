package com.omar.beautycenter.closing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReopenDailyRequest(@NotBlank @Size(max = 500) String reason) {
}

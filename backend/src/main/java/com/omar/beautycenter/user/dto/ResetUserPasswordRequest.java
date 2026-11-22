package com.omar.beautycenter.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetUserPasswordRequest(
        @NotBlank(message = "Password is required.")
        @Size(max = 200, message = "Password must not exceed 200 characters.")
        String password) {
}

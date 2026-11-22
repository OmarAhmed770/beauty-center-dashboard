package com.omar.beautycenter.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StaffRegistrationRequest(
        @NotBlank(message = "Full name is required.")
        @Size(max = 150, message = "Full name must not exceed 150 characters.")
        String displayName,
        @NotBlank(message = "Username is required.")
        @Size(max = 100, message = "Username must not exceed 100 characters.")
        String username,
        @NotBlank(message = "Password is required.")
        @Size(max = 200, message = "Password must not exceed 200 characters.")
        String password,
        @NotBlank(message = "Password confirmation is required.")
        @Size(max = 200, message = "Password confirmation must not exceed 200 characters.")
        String passwordConfirmation,
        @Size(max = 50, message = "Phone must not exceed 50 characters.")
        String phone) {
}

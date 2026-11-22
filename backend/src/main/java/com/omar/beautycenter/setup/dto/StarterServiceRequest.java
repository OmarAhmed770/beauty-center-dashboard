package com.omar.beautycenter.setup.dto;

import com.omar.beautycenter.setup.StarterServiceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StarterServiceRequest(
        @NotBlank(message = "Starter service name is required.")
        @Size(max = 150, message = "Starter service name must not exceed 150 characters.")
        String nameArabic,
        @NotNull(message = "Starter service category is required.")
        StarterServiceCategory category) {
}

package com.omar.beautycenter.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateCatalogItemRequest(
        @NotBlank(message = "Arabic name is required.")
        @Size(max = 150, message = "Arabic name must not exceed 150 characters.")
        String nameArabic,

        @NotBlank(message = "English name is required.")
        @Size(max = 150, message = "English name must not exceed 150 characters.")
        String nameEnglish,

        @NotNull(message = "Category id is required.")
        @Positive(message = "Category id must be positive.")
        Long categoryId,

        @Size(max = 2000, message = "Description must not exceed 2000 characters.")
        String description) {
}

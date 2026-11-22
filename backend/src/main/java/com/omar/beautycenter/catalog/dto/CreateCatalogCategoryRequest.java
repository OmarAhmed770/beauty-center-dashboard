package com.omar.beautycenter.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCatalogCategoryRequest(
        @NotBlank(message = "Arabic name is required.")
        @Size(max = 150, message = "Arabic name must not exceed 150 characters.")
        String nameArabic,

        @NotBlank(message = "English name is required.")
        @Size(max = 150, message = "English name must not exceed 150 characters.")
        String nameEnglish,

        @NotNull(message = "Active status is required.")
        Boolean active,

        @Size(max = 1000, message = "Description must not exceed 1000 characters.")
        String description) {
}

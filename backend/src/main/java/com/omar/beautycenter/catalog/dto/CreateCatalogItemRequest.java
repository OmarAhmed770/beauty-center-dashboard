package com.omar.beautycenter.catalog.dto;

import com.omar.beautycenter.catalog.CatalogItemType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateCatalogItemRequest(
        @NotBlank(message = "Code is required.")
        @Size(max = 50, message = "Code must not exceed 50 characters.")
        @Pattern(regexp = "[A-Za-z0-9_]+", message = "Code may contain only letters, numbers, and underscores.")
        String code,

        @NotBlank(message = "Arabic name is required.")
        @Size(max = 150, message = "Arabic name must not exceed 150 characters.")
        String nameArabic,

        @NotBlank(message = "English name is required.")
        @Size(max = 150, message = "English name must not exceed 150 characters.")
        String nameEnglish,

        @NotNull(message = "Item type is required.")
        CatalogItemType itemType,

        @NotNull(message = "Category id is required.")
        @Positive(message = "Category id must be positive.")
        Long categoryId,

        @NotNull(message = "Selling price is required.")
        @PositiveOrZero(message = "Selling price must be zero or greater.")
        @Digits(integer = 17, fraction = 2, message = "Selling price must have at most 2 decimal digits.")
        BigDecimal currentSellingPrice,

        @NotNull(message = "Cost is required.")
        @PositiveOrZero(message = "Cost must be zero or greater.")
        @Digits(integer = 17, fraction = 2, message = "Cost must have at most 2 decimal digits.")
        BigDecimal currentCost,

        @NotNull(message = "Active status is required.")
        Boolean active,

        @Size(max = 2000, message = "Description must not exceed 2000 characters.")
        String description) {
}

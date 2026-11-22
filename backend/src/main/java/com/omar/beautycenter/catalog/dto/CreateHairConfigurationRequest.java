package com.omar.beautycenter.catalog.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateHairConfigurationRequest(
        @NotNull(message = "Hair item id is required.")
        @Positive(message = "Hair item id must be positive.")
        Long hairItemId,

        @NotBlank(message = "Brand is required.")
        @Size(max = 150, message = "Brand must not exceed 150 characters.")
        String brand,

        @NotBlank(message = "Length is required.")
        @Size(max = 100, message = "Length must not exceed 100 characters.")
        String length,

        @Size(max = 100, message = "Color must not exceed 100 characters.")
        String color,

        @NotBlank(message = "Unit type is required.")
        @Size(max = 50, message = "Unit type must not exceed 50 characters.")
        String unitType,

        @NotNull(message = "Unit selling price is required.")
        @PositiveOrZero(message = "Unit selling price must be zero or greater.")
        @Digits(integer = 17, fraction = 2, message = "Unit selling price must have at most 2 decimal digits.")
        BigDecimal unitSellingPrice,

        @NotNull(message = "Unit cost is required.")
        @PositiveOrZero(message = "Unit cost must be zero or greater.")
        @Digits(integer = 17, fraction = 2, message = "Unit cost must have at most 2 decimal digits.")
        BigDecimal unitCost,

        @NotNull(message = "Installation fee is required.")
        @PositiveOrZero(message = "Installation fee must be zero or greater.")
        @Digits(integer = 17, fraction = 2, message = "Installation fee must have at most 2 decimal digits.")
        BigDecimal installationFee,

        @Positive(message = "Minimum quantity must be greater than zero.")
        @Digits(integer = 16, fraction = 3, message = "Minimum quantity must have at most 3 decimal digits.")
        BigDecimal minimumQuantity,

        @NotNull(message = "Active status is required.")
        Boolean active,

        @NotNull(message = "Effective date is required.")
        LocalDate effectiveDate) {
}

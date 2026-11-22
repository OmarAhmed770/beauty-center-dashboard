package com.omar.beautycenter.catalog.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateCatalogPriceRequest(
        @PositiveOrZero(message = "Selling price must be zero or greater.")
        @Digits(integer = 17, fraction = 2, message = "Selling price must have at most 2 decimal digits.")
        BigDecimal sellingPrice,

        @PositiveOrZero(message = "Cost must be zero or greater.")
        @Digits(integer = 17, fraction = 2, message = "Cost must have at most 2 decimal digits.")
        BigDecimal cost,

        @NotBlank(message = "Change reason is required.")
        @Size(max = 500, message = "Change reason must not exceed 500 characters.")
        String reason) {
}

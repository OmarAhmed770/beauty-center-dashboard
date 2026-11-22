package com.omar.beautycenter.catalog.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record InitializeHairStockRequest(
        @NotNull(message = "Initial stock quantity is required.")
        @PositiveOrZero(message = "Initial stock quantity cannot be negative.")
        @Digits(integer = 16, fraction = 3, message = "Initial stock quantity must have at most 3 decimal digits.")
        BigDecimal quantityGrams,

        @NotBlank(message = "Stock reason is required.")
        @Size(max = 500, message = "Stock reason must not exceed 500 characters.")
        String reason) {
}

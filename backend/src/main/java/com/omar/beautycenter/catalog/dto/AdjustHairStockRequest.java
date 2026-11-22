package com.omar.beautycenter.catalog.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AdjustHairStockRequest(
        @NotNull(message = "Stock adjustment is required.")
        @Digits(integer = 16, fraction = 3, message = "Stock adjustment must have at most 3 decimal digits.")
        BigDecimal adjustmentGrams,

        @NotBlank(message = "Stock reason is required.")
        @Size(max = 500, message = "Stock reason must not exceed 500 characters.")
        String reason) {
}

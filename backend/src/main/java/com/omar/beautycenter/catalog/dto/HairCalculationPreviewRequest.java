package com.omar.beautycenter.catalog.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record HairCalculationPreviewRequest(
        @NotNull(message = "Configuration id is required.")
        @Positive(message = "Configuration id must be positive.")
        Long configurationId,

        @NotNull(message = "Quantity is required.")
        @Positive(message = "Quantity must be greater than zero.")
        @Digits(integer = 16, fraction = 3, message = "Quantity must have at most 3 decimal digits.")
        BigDecimal quantity,

        @Positive(message = "Discount id must be positive.")
        Long discountId,

        @PositiveOrZero(message = "Override amount must be zero or greater.")
        @Digits(integer = 17, fraction = 2, message = "Override amount must have at most 2 decimal digits.")
        BigDecimal overrideAmount,

        @Size(max = 500, message = "Override reason must not exceed 500 characters.")
        String overrideReason) {
}

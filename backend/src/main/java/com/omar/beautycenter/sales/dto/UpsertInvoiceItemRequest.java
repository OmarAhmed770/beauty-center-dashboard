package com.omar.beautycenter.sales.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpsertInvoiceItemRequest(
        @NotNull(message = "Catalog item id is required.")
        @Positive(message = "Catalog item id must be positive.")
        Long catalogItemId,

        @Positive(message = "Hair configuration id must be positive.")
        Long hairConfigurationId,

        @NotNull(message = "Quantity is required.")
        @Positive(message = "Quantity must be greater than zero.")
        @Digits(integer = 16, fraction = 3, message = "Quantity must have at most 3 decimal digits.")
        BigDecimal quantity,

        @Positive(message = "Discount id must be positive.")
        Long discountId,

        @Positive(message = "Employee id must be positive.")
        Long employeeId,

        @PositiveOrZero(message = "Price override amount must be zero or greater.")
        @Digits(integer = 17, fraction = 2, message = "Price override amount must have at most 2 decimal digits.")
        BigDecimal priceOverrideAmount,

        @Size(max = 500, message = "Price override reason must not exceed 500 characters.")
        String priceOverrideReason) {
}

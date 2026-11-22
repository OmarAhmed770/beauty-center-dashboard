package com.omar.beautycenter.sales.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Optional physical return details for a financial invoice refund. */
public record HairRefundReturnRequest(
        @NotNull(message = "Invoice item is required for returned hair.")
        Long invoiceItemId,

        @NotNull(message = "Returned hair quantity is required.")
        @Positive(message = "Returned hair quantity must be greater than zero.")
        @Digits(integer = 16, fraction = 3, message = "Returned hair quantity must have at most 3 decimal digits.")
        BigDecimal grams) {
}

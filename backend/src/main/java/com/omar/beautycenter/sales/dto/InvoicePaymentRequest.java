package com.omar.beautycenter.sales.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record InvoicePaymentRequest(
        @NotNull(message = "Payment method id is required.")
        @Positive(message = "Payment method id must be positive.")
        Long paymentMethodId,

        @NotNull(message = "Payment amount is required.")
        @Positive(message = "Payment amount must be greater than zero.")
        @Digits(integer = 17, fraction = 2, message = "Payment amount must have at most 2 decimal digits.")
        BigDecimal amount,

        @Size(max = 200, message = "Reference number must not exceed 200 characters.")
        String referenceNumber) {
}

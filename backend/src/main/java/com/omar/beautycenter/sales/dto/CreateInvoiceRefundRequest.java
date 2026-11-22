package com.omar.beautycenter.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateInvoiceRefundRequest(
        @NotNull(message = "Business date is required.")
        LocalDate businessDate,

        @NotNull(message = "Refund amount is required.")
        @Positive(message = "Refund amount must be greater than zero.")
        @Digits(integer = 17, fraction = 2, message = "Refund amount must have at most 2 decimal digits.")
        BigDecimal amount,

        @NotBlank(message = "Refund reason is required.")
        @Size(max = 500, message = "Refund reason must not exceed 500 characters.")
        String reason,

        @NotEmpty(message = "At least one refund payment is required.")
        @Size(max = 20, message = "A refund cannot contain more than 20 payments.")
        List<@Valid InvoicePaymentRequest> payments,

        @Size(max = 100, message = "A refund cannot contain more than 100 returned hair lines.")
        List<@Valid HairRefundReturnRequest> hairReturns) {

    public CreateInvoiceRefundRequest(
            LocalDate businessDate,
            BigDecimal amount,
            String reason,
            List<InvoicePaymentRequest> payments) {
        this(businessDate, amount, reason, payments, null);
    }
}

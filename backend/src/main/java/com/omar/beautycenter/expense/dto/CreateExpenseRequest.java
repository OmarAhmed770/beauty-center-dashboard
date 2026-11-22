package com.omar.beautycenter.expense.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExpenseRequest(
        @NotNull(message = "Category id is required.")
        @Positive(message = "Category id must be positive.")
        Long categoryId,

        @NotNull(message = "Amount is required.")
        @Positive(message = "Amount must be greater than zero.")
        @Digits(integer = 17, fraction = 2, message = "Amount must have at most 17 integer and 2 decimal digits.")
        BigDecimal amount,

        @NotNull(message = "Business date is required.")
        LocalDate businessDate,

        @NotNull(message = "Payment method id is required.")
        @Positive(message = "Payment method id must be positive.")
        Long paymentMethodId,

        @Size(max = 200, message = "Payment reference must not exceed 200 characters.")
        String paymentReference,

        @NotBlank(message = "Description is required.")
        @Size(max = 1000, message = "Description must not exceed 1000 characters.")
        String description,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters.")
        String notes,

        @Size(max = 500, message = "Receipt path must not exceed 500 characters.")
        String receiptPath) {
}

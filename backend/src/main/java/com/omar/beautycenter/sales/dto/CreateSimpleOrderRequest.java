package com.omar.beautycenter.sales.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One-line manual sale that is persisted through the normal invoice lifecycle.
 *
 * <p>{@code paymentReference} stays optional at the transport level because only configured payment
 * methods that require it need a value. The shared payment validation enforces that requirement.
 */
public record CreateSimpleOrderRequest(
        @Size(max = 150, message = "Customer name must not exceed 150 characters.")
        String customerName,

        @Size(max = 50, message = "Customer phone must not exceed 50 characters.")
        String customerPhone,

        @NotNull(message = "Business date is required.")
        LocalDate businessDate,

        @NotBlank(message = "Description is required.")
        @Size(max = 150, message = "Description must not exceed 150 characters.")
        String description,

        @NotNull(message = "Total amount is required.")
        @Positive(message = "Total amount must be greater than zero.")
        @Digits(integer = 17, fraction = 2, message = "Total amount must have at most 2 decimal digits.")
        BigDecimal totalAmount,

        @NotNull(message = "Payment method id is required.")
        @Positive(message = "Payment method id must be positive.")
        Long paymentMethodId,

        @Size(max = 200, message = "Reference number must not exceed 200 characters.")
        String paymentReference,

        @Positive(message = "Employee id must be positive.")
        Long employeeId,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters.")
        String notes) {
}

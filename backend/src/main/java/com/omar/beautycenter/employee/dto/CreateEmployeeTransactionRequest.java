package com.omar.beautycenter.employee.dto;

import com.omar.beautycenter.employee.EmployeeTransactionType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateEmployeeTransactionRequest(
        @NotNull(message = "Employee id is required.")
        @Positive(message = "Employee id must be positive.")
        Long employeeId,

        @NotNull(message = "Transaction type is required.")
        EmployeeTransactionType type,

        @NotNull(message = "Amount is required.")
        @Positive(message = "Amount must be greater than zero.")
        @Digits(integer = 17, fraction = 2, message = "Amount must have at most 17 integer and 2 decimal digits.")
        BigDecimal amount,

        @NotNull(message = "Business date is required.")
        LocalDate businessDate,

        @Size(max = 100, message = "Payment method must not exceed 100 characters.")
        String paymentMethod,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters.")
        String notes) {
}

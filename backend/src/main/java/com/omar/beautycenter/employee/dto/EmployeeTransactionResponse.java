package com.omar.beautycenter.employee.dto;

import com.omar.beautycenter.employee.EmployeeTransaction;
import com.omar.beautycenter.employee.EmployeeTransactionStatus;
import com.omar.beautycenter.employee.EmployeeTransactionType;
import com.omar.beautycenter.payment.PaymentMethodType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeTransactionResponse(
        Long id,
        Long employeeId,
        String employeeName,
        EmployeeTransactionType type,
        BigDecimal amount,
        LocalDate businessDate,
        Long paymentMethodId,
        String paymentMethod,
        PaymentMethodType paymentMethodType,
        String notes,
        EmployeeTransactionStatus status,
        LocalDateTime cancelledAt,
        String cancelledBy,
        String cancellationReason,
        long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy) {

    public static EmployeeTransactionResponse from(EmployeeTransaction transaction) {
        return new EmployeeTransactionResponse(
                transaction.getId(),
                transaction.getEmployee().getId(),
                transaction.getEmployee().getName(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBusinessDate(),
                transaction.getPaymentMethodReference() == null
                        ? null
                        : transaction.getPaymentMethodReference().getId(),
                transaction.getPaymentMethod(),
                transaction.getPaymentMethodType(),
                transaction.getNotes(),
                transaction.getStatus(),
                transaction.getCancelledAt(),
                transaction.getCancelledBy(),
                transaction.getCancellationReason(),
                transaction.getVersion(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt(),
                transaction.getCreatedBy(),
                transaction.getUpdatedBy());
    }
}

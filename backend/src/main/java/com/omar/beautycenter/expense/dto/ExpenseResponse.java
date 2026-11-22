package com.omar.beautycenter.expense.dto;

import com.omar.beautycenter.expense.Expense;
import com.omar.beautycenter.expense.ExpenseStatus;
import com.omar.beautycenter.payment.PaymentMethodType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseResponse(
        Long id,
        Long categoryId,
        String categoryNameArabic,
        String categoryNameEnglish,
        BigDecimal amount,
        LocalDate businessDate,
        Long paymentMethodId,
        String paymentMethodCode,
        String paymentMethodNameArabic,
        String paymentMethodNameEnglish,
        PaymentMethodType paymentMethodType,
        String paymentReference,
        String description,
        String notes,
        String receiptPath,
        ExpenseStatus status,
        LocalDateTime cancelledAt,
        String cancelledBy,
        String cancellationReason,
        long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy) {

    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getCategory().getId(),
                expense.getCategoryNameArabic(),
                expense.getCategoryNameEnglish(),
                expense.getAmount(),
                expense.getBusinessDate(),
                expense.getPaymentMethod().getId(),
                expense.getPaymentMethodCode(),
                expense.getPaymentMethodNameArabic(),
                expense.getPaymentMethodNameEnglish(),
                expense.getPaymentMethodType(),
                expense.getPaymentReference(),
                expense.getDescription(),
                expense.getNotes(),
                expense.getReceiptPath(),
                expense.getStatus(),
                expense.getCancelledAt(),
                expense.getCancelledBy(),
                expense.getCancellationReason(),
                expense.getVersion(),
                expense.getCreatedAt(),
                expense.getUpdatedAt(),
                expense.getCreatedBy(),
                expense.getUpdatedBy());
    }
}

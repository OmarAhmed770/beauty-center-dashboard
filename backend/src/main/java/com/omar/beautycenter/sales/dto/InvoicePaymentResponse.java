package com.omar.beautycenter.sales.dto;

import com.omar.beautycenter.payment.PaymentMethodType;
import com.omar.beautycenter.sales.InvoicePayment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoicePaymentResponse(
        Long id,
        Long paymentMethodId,
        String paymentMethodCode,
        String paymentMethodNameArabic,
        String paymentMethodNameEnglish,
        PaymentMethodType paymentMethodType,
        BigDecimal amount,
        String referenceNumber,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy) {

    public static InvoicePaymentResponse from(InvoicePayment payment) {
        return new InvoicePaymentResponse(
                payment.getId(),
                payment.getPaymentMethod().getId(),
                payment.getPaymentMethodCodeSnapshot(),
                payment.getPaymentMethodNameArabicSnapshot(),
                payment.getPaymentMethodNameEnglishSnapshot(),
                payment.getPaymentMethodTypeSnapshot(),
                payment.getAmount(),
                payment.getReferenceNumber(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getCreatedBy(),
                payment.getUpdatedBy());
    }
}

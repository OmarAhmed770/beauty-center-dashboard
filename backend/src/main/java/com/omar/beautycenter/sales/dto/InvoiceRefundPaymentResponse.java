package com.omar.beautycenter.sales.dto;

import com.omar.beautycenter.payment.PaymentMethodType;
import com.omar.beautycenter.sales.InvoiceRefundPayment;
import java.math.BigDecimal;

public record InvoiceRefundPaymentResponse(
        Long id,
        Long paymentMethodId,
        String paymentMethodCode,
        String paymentMethodNameArabic,
        String paymentMethodNameEnglish,
        PaymentMethodType paymentMethodType,
        BigDecimal amount,
        String referenceNumber) {

    public static InvoiceRefundPaymentResponse from(InvoiceRefundPayment payment) {
        return new InvoiceRefundPaymentResponse(
                payment.getId(),
                payment.getPaymentMethod().getId(),
                payment.getPaymentMethodCodeSnapshot(),
                payment.getPaymentMethodNameArabicSnapshot(),
                payment.getPaymentMethodNameEnglishSnapshot(),
                payment.getPaymentMethodTypeSnapshot(),
                payment.getAmount(),
                payment.getReferenceNumber());
    }
}

package com.omar.beautycenter.report.dto;

import com.omar.beautycenter.payment.PaymentMethodType;
import java.math.BigDecimal;

public record PaymentMethodFinancialTotalResponse(
        Long paymentMethodId,
        String code,
        String displayNameArabic,
        String displayNameEnglish,
        PaymentMethodType type,
        BigDecimal receivedAmount,
        BigDecimal refundedAmount,
        BigDecimal netAmount) {
}

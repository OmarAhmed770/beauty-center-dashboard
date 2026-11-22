package com.omar.beautycenter.expense.dto;

import com.omar.beautycenter.payment.PaymentMethodType;
import java.math.BigDecimal;

public record ExpensePaymentMethodTotalResponse(
        Long paymentMethodId,
        String code,
        String displayNameArabic,
        String displayNameEnglish,
        PaymentMethodType type,
        BigDecimal totalAmount,
        long expenseCount) {
}

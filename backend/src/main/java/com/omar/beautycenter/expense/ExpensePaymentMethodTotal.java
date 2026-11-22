package com.omar.beautycenter.expense;

import com.omar.beautycenter.payment.PaymentMethodType;
import java.math.BigDecimal;

public interface ExpensePaymentMethodTotal {

    Long getPaymentMethodId();

    String getCode();

    String getDisplayNameArabic();

    String getDisplayNameEnglish();

    PaymentMethodType getType();

    BigDecimal getTotalAmount();

    long getExpenseCount();
}

package com.omar.beautycenter.expense;

import java.math.BigDecimal;

public interface ExpenseCategoryTotal {

    Long getCategoryId();

    String getNameArabic();

    String getNameEnglish();

    BigDecimal getTotalAmount();

    long getExpenseCount();
}

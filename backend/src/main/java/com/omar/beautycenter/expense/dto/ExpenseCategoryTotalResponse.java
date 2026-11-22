package com.omar.beautycenter.expense.dto;

import java.math.BigDecimal;

public record ExpenseCategoryTotalResponse(
        Long categoryId,
        String nameArabic,
        String nameEnglish,
        BigDecimal totalAmount,
        long expenseCount) {
}

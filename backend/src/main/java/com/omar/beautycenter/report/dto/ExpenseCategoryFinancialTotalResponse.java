package com.omar.beautycenter.report.dto;

import java.math.BigDecimal;

public record ExpenseCategoryFinancialTotalResponse(
        Long categoryId,
        String nameArabic,
        String nameEnglish,
        BigDecimal amount,
        long expenseCount) {
}

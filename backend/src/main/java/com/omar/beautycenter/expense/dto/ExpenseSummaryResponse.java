package com.omar.beautycenter.expense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseSummaryResponse(
        LocalDate fromDate,
        LocalDate toDate,
        long activeExpenseCount,
        long cancelledExpenseCount,
        BigDecimal totalAmount,
        BigDecimal cashAmount,
        BigDecimal nonCashAmount) {
}

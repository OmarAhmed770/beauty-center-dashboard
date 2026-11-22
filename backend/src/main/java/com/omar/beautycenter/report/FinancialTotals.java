package com.omar.beautycenter.report;

import java.math.BigDecimal;

public record FinancialTotals(
        BigDecimal grossSales,
        BigDecimal discounts,
        BigDecimal refunds,
        BigDecimal netSales,
        long completedInvoiceCount,
        BigDecimal expenses,
        BigDecimal netSalesAfterExpenses,
        BigDecimal dailyWages,
        BigDecimal bonuses,
        BigDecimal commissions,
        BigDecimal employeeCost,
        BigDecimal advances,
        BigDecimal employeePayments,
        BigDecimal costOfSoldItems,
        BigDecimal netProfit,
        BigDecimal cashReceived,
        BigDecimal cashExpenses,
        BigDecimal cashEmployeeOutflows,
        BigDecimal cashRefunds,
        BigDecimal cashAdjustments,
        BigDecimal expectedCash) {
}

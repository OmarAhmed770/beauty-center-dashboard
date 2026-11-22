package com.omar.beautycenter.report;

import java.math.BigDecimal;

public record FinancialInputs(
        BigDecimal grossSales,
        BigDecimal discounts,
        BigDecimal refunds,
        long completedInvoiceCount,
        BigDecimal expenses,
        BigDecimal dailyWages,
        BigDecimal bonuses,
        BigDecimal commissions,
        BigDecimal advances,
        BigDecimal employeePayments,
        BigDecimal costOfSoldItems,
        BigDecimal cashReceived,
        BigDecimal cashExpenses,
        BigDecimal cashEmployeeOutflows,
        BigDecimal cashRefunds,
        BigDecimal cashAdjustments) {
}

package com.omar.beautycenter.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FinancialSummaryResponse(
        LocalDate fromDate,
        LocalDate toDate,
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
        BigDecimal costOfSoldItemsAndHair,
        BigDecimal netProfit,
        BigDecimal cashPaymentsReceived,
        BigDecimal cashExpenses,
        BigDecimal cashEmployeePayments,
        BigDecimal cashRefunds,
        BigDecimal cashAdjustments,
        BigDecimal expectedCashBalance,
        List<PaymentMethodFinancialTotalResponse> paymentTotalsByMethod,
        List<ExpenseCategoryFinancialTotalResponse> expenseTotalsByCategory,
        List<TopCatalogItemResponse> topServicesAndProducts,
        List<TopEmployeeResponse> topPerformingEmployees,
        List<DailySalesTrendResponse> dailySalesTrend) {
}

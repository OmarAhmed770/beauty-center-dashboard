package com.omar.beautycenter.closing.dto;

import com.omar.beautycenter.closing.DailyClosing;
import com.omar.beautycenter.closing.DailyClosingExpenseTotal;
import com.omar.beautycenter.closing.DailyClosingPaymentTotal;
import com.omar.beautycenter.closing.DailyClosingStatus;
import com.omar.beautycenter.report.dto.ExpenseCategoryFinancialTotalResponse;
import com.omar.beautycenter.report.dto.PaymentMethodFinancialTotalResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DailyClosingResponse(
        Long id,
        LocalDate businessDate,
        int closingVersion,
        DailyClosingStatus status,
        BigDecimal grossSales,
        BigDecimal discounts,
        BigDecimal refunds,
        BigDecimal netSales,
        long completedInvoiceCount,
        BigDecimal expenseTotal,
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
        BigDecimal expectedCash,
        BigDecimal actualCash,
        BigDecimal cashDifference,
        List<PaymentMethodFinancialTotalResponse> paymentMethodBreakdown,
        List<ExpenseCategoryFinancialTotalResponse> expenseCategoryBreakdown,
        String closedBy,
        LocalDateTime closedAt,
        String notes,
        String reopenedBy,
        LocalDateTime reopenedAt,
        String reopeningReason,
        int reopenCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static DailyClosingResponse from(
            DailyClosing closing,
            List<DailyClosingPaymentTotal> payments,
            List<DailyClosingExpenseTotal> expenses) {
        return new DailyClosingResponse(
                closing.getId(), closing.getBusinessDate(), closing.getClosingVersion(), closing.getStatus(), closing.getGrossSales(),
                closing.getDiscounts(), closing.getRefunds(), closing.getNetSales(),
                closing.getCompletedInvoiceCount(), closing.getExpenseTotal(), closing.getDailyWages(),
                closing.getBonuses(), closing.getCommissions(), closing.getEmployeeCost(), closing.getAdvances(),
                closing.getEmployeePayments(), closing.getCostOfSoldItems(), closing.getNetProfit(),
                closing.getCashReceived(), closing.getCashExpenses(), closing.getCashEmployeeOutflows(),
                closing.getCashRefunds(), closing.getCashAdjustments(), closing.getExpectedCash(),
                closing.getActualCash(), closing.getCashDifference(),
                payments.stream().map(total -> new PaymentMethodFinancialTotalResponse(
                        total.getPaymentMethodId(), total.getCode(), total.getNameArabic(), total.getNameEnglish(),
                        total.getType(), total.getReceivedAmount(), total.getRefundedAmount(), total.getNetAmount()))
                        .toList(),
                expenses.stream().map(total -> new ExpenseCategoryFinancialTotalResponse(
                        total.getCategoryId(), total.getNameArabic(), total.getNameEnglish(), total.getAmount(),
                        total.getExpenseCount())).toList(),
                closing.getClosedBy(), closing.getClosedAt(), closing.getNotes(), closing.getReopenedBy(),
                closing.getReopenedAt(), closing.getReopeningReason(), closing.getReopenCount(),
                closing.getCreatedAt(), closing.getUpdatedAt());
    }
}

package com.omar.beautycenter.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class FinancialFormulaService {

    public FinancialTotals calculate(FinancialInputs input) {
        BigDecimal grossSales = money(input.grossSales());
        BigDecimal discounts = money(input.discounts());
        BigDecimal refunds = money(input.refunds());
        BigDecimal expenses = money(input.expenses());
        BigDecimal dailyWages = money(input.dailyWages());
        BigDecimal bonuses = money(input.bonuses());
        BigDecimal commissions = money(input.commissions());
        BigDecimal advances = money(input.advances());
        BigDecimal employeePayments = money(input.employeePayments());
        BigDecimal costOfSoldItems = money(input.costOfSoldItems());
        BigDecimal cashReceived = money(input.cashReceived());
        BigDecimal cashExpenses = money(input.cashExpenses());
        BigDecimal cashEmployeeOutflows = money(input.cashEmployeeOutflows());
        BigDecimal cashRefunds = money(input.cashRefunds());
        BigDecimal cashAdjustments = money(input.cashAdjustments());

        BigDecimal netSales = grossSales.subtract(discounts).subtract(refunds);
        BigDecimal netSalesAfterExpenses = netSales.subtract(expenses);
        // Advances and settlement payments change the employee balance/cash only. They must never
        // be deducted again from profit after daily wages, bonuses, and commissions are recorded.
        BigDecimal employeeCost = dailyWages.add(bonuses).add(commissions);
        BigDecimal netProfit = netSales
                .subtract(costOfSoldItems)
                .subtract(expenses)
                .subtract(employeeCost);
        // cashAdjustments includes the existing CASH_IN mechanism for a starting cash float
        // (and other audited drawer corrections), so closing and reports use one cash source.
        BigDecimal expectedCash = cashReceived
                .subtract(cashExpenses)
                .subtract(cashEmployeeOutflows)
                .subtract(cashRefunds)
                .add(cashAdjustments);

        return new FinancialTotals(
                grossSales,
                discounts,
                refunds,
                money(netSales),
                input.completedInvoiceCount(),
                expenses,
                money(netSalesAfterExpenses),
                dailyWages,
                bonuses,
                commissions,
                money(employeeCost),
                advances,
                employeePayments,
                costOfSoldItems,
                money(netProfit),
                cashReceived,
                cashExpenses,
                cashEmployeeOutflows,
                cashRefunds,
                cashAdjustments,
                money(expectedCash));
    }

    static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}

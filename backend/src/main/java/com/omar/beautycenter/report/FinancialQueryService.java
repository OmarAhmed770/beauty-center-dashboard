package com.omar.beautycenter.report;

import com.omar.beautycenter.report.dto.FinancialSummaryResponse;
import com.omar.beautycenter.report.dto.ProfitReportResponse;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialQueryService {

    private static final int TOP_RESULT_LIMIT = 10;

    private final DateRangeResolver dateRangeResolver;
    private final FinancialDataQueryStore queryStore;
    private final FinancialFormulaService formulaService;

    public FinancialQueryService(
            DateRangeResolver dateRangeResolver,
            FinancialDataQueryStore queryStore,
            FinancialFormulaService formulaService) {
        this.dateRangeResolver = dateRangeResolver;
        this.queryStore = queryStore;
        this.formulaService = formulaService;
    }

    @Transactional(readOnly = true)
    public FinancialSummaryResponse dashboard(
            DashboardFilter filter, LocalDate customFrom, LocalDate customTo) {
        DateRange range = dateRangeResolver.resolve(filter, customFrom, customTo);
        return summary(range.fromDate(), range.toDate());
    }

    @Transactional(readOnly = true)
    public FinancialSummaryResponse summary(LocalDate fromDate, LocalDate toDate) {
        DateRange range = dateRangeResolver.required(fromDate, toDate);
        FinancialTotals totals = formulaService.calculate(
                queryStore.loadInputs(range.fromDate(), range.toDate()));
        return new FinancialSummaryResponse(
                range.fromDate(), range.toDate(), totals.grossSales(), totals.discounts(), totals.refunds(),
                totals.netSales(), totals.completedInvoiceCount(), totals.expenses(), totals.netSalesAfterExpenses(),
                totals.dailyWages(),
                totals.bonuses(), totals.commissions(), totals.employeeCost(), totals.advances(),
                totals.employeePayments(), totals.costOfSoldItems(), totals.netProfit(), totals.cashReceived(),
                totals.cashExpenses(), totals.cashEmployeeOutflows(), totals.cashRefunds(),
                totals.cashAdjustments(), totals.expectedCash(),
                queryStore.paymentTotals(range.fromDate(), range.toDate()),
                queryStore.expenseTotals(range.fromDate(), range.toDate()),
                queryStore.topItems(range.fromDate(), range.toDate(), TOP_RESULT_LIMIT),
                queryStore.topEmployees(range.fromDate(), range.toDate(), TOP_RESULT_LIMIT),
                queryStore.salesTrend(range.fromDate(), range.toDate()));
    }

    @Transactional(readOnly = true)
    public ProfitReportResponse profit(LocalDate fromDate, LocalDate toDate) {
        FinancialSummaryResponse summary = summary(fromDate, toDate);
        return new ProfitReportResponse(summary.fromDate(), summary.toDate(), summary.netSales(),
                summary.costOfSoldItemsAndHair(), summary.expenses(), summary.employeeCost(),
                summary.netProfit());
    }
}

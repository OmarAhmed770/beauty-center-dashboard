package com.omar.beautycenter.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FinancialFormulaServiceTest {

    private final FinancialFormulaService service = new FinancialFormulaService();

    @Test
    void noActivityDayReturnsNormalizedZeros() {
        FinancialTotals result = service.calculate(new FinancialInputs(
                null, null, null, 0, null, null, null, null, null, null, null,
                null, null, null, null, null));

        assertThat(result.netSales()).isEqualByComparingTo("0.00");
        assertThat(result.netSalesAfterExpenses()).isEqualByComparingTo("0.00");
        assertThat(result.employeeCost()).isEqualByComparingTo("0.00");
        assertThat(result.netProfit()).isEqualByComparingTo("0.00");
        assertThat(result.expectedCash()).isEqualByComparingTo("0.00");
    }

    @Test
    void formulasSeparateProfitFromEmployeeBalanceAndCashFlows() {
        FinancialTotals result = service.calculate(new FinancialInputs(
                money("1000"), money("100"), money("50"), 2,
                money("120"), money("40"), money("10"), money("5"),
                money("30"), money("25"), money("200"),
                money("500"), money("60"), money("25"), money("20"), money("15")));

        assertThat(result.netSales()).isEqualByComparingTo("850.00");
        assertThat(result.netSalesAfterExpenses()).isEqualByComparingTo("730.00");
        assertThat(result.employeeCost()).isEqualByComparingTo("55.00");
        assertThat(result.netProfit()).isEqualByComparingTo("475.00");
        assertThat(result.expectedCash()).isEqualByComparingTo("410.00");
        assertThat(result.advances()).isEqualByComparingTo("30.00");
        assertThat(result.employeePayments()).isEqualByComparingTo("25.00");
    }

    @Test
    void dailyWagesCommissionsAndOperatingExpensesAreEachDeductedOnce() {
        FinancialTotals result = service.calculate(new FinancialInputs(
                money("1000"), money("0"), money("0"), 1,
                money("120"), money("100"), money("20"), money("50"),
                money("300"), money("170"), money("80"),
                money("1000"), money("120"), money("170"), money("0"), money("0")));

        assertThat(result.employeeCost()).isEqualByComparingTo("170.00");
        assertThat(result.netProfit()).isEqualByComparingTo("630.00");
        assertThat(result.advances()).isEqualByComparingTo("300.00");
        assertThat(result.employeePayments()).isEqualByComparingTo("170.00");
        assertThat(result.expectedCash()).isEqualByComparingTo("710.00");
    }

    @Test
    void expectedCashUsesTheOpeningCashAdjustmentAndOnlyActualCashOutflows() {
        FinancialTotals result = service.calculate(new FinancialInputs(
                money("300"), money("0"), money("0"), 1,
                money("40"), money("100"), money("20"), money("30"),
                money("25"), money("10"), money("0"),
                money("300"), money("40"), money("85"), money("15"), money("50")));

        assertThat(result.employeeCost()).isEqualByComparingTo("150.00");
        assertThat(result.netProfit()).isEqualByComparingTo("110.00");
        assertThat(result.expectedCash()).isEqualByComparingTo("210.00");
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}

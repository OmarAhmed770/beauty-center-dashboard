package com.omar.beautycenter.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProfitReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal netSales,
        BigDecimal costOfSoldItemsAndHair,
        BigDecimal operatingExpenses,
        BigDecimal employeeCost,
        BigDecimal netProfit) {
}

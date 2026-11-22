package com.omar.beautycenter.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySalesTrendResponse(
        LocalDate businessDate,
        BigDecimal grossSales,
        BigDecimal discounts,
        BigDecimal refunds,
        BigDecimal netSales,
        long completedInvoiceCount) {
}

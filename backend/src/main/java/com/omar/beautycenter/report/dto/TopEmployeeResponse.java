package com.omar.beautycenter.report.dto;

import java.math.BigDecimal;

public record TopEmployeeResponse(
        Long employeeId,
        String employeeName,
        BigDecimal quantity,
        BigDecimal netRevenue,
        long invoiceCount) {
}

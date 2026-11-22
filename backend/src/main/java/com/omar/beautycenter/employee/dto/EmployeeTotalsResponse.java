package com.omar.beautycenter.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeTotalsResponse(
        Long employeeId,
        String employeeName,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal dailyWages,
        BigDecimal bonuses,
        BigDecimal commissions,
        BigDecimal deductions,
        BigDecimal advances,
        BigDecimal payments,
        BigDecimal netEntitlement) {
}

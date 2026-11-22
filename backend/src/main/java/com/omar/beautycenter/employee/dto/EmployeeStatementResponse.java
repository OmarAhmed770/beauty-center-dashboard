package com.omar.beautycenter.employee.dto;

import com.omar.beautycenter.common.dto.PageResponse;
import java.time.LocalDate;

public record EmployeeStatementResponse(
        EmployeeResponse employee,
        LocalDate fromDate,
        LocalDate toDate,
        EmployeeTotalsResponse totals,
        PageResponse<EmployeeTransactionResponse> transactions) {
}

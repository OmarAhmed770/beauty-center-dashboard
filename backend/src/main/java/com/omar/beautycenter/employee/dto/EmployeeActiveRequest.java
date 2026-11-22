package com.omar.beautycenter.employee.dto;

import jakarta.validation.constraints.NotNull;

public record EmployeeActiveRequest(
        @NotNull(message = "Active status is required.") Boolean active) {
}

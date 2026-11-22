package com.omar.beautycenter.employee.dto;

import com.omar.beautycenter.employee.Employee;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EmployeeResponse(
        Long id,
        String name,
        String phone,
        String position,
        BigDecimal defaultDailyWage,
        boolean active,
        String notes,
        long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy) {

    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getPhone(),
                employee.getPosition(),
                employee.getDefaultDailyWage(),
                employee.isActive(),
                employee.getNotes(),
                employee.getVersion(),
                employee.getCreatedAt(),
                employee.getUpdatedAt(),
                employee.getCreatedBy(),
                employee.getUpdatedBy());
    }
}

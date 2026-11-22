package com.omar.beautycenter.employee.dto;

import com.omar.beautycenter.employee.Employee;
import com.omar.beautycenter.employee.EmployeeAttendance;
import com.omar.beautycenter.employee.EmployeeAttendanceStatus;
import com.omar.beautycenter.employee.EmployeeTransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeAttendanceResponse(
        Long id,
        Long employeeId,
        String employeeName,
        boolean employeeActive,
        LocalDate businessDate,
        EmployeeAttendanceStatus status,
        BigDecimal dailyWageAmount,
        Long dailyWageTransactionId,
        EmployeeTransactionStatus dailyWageTransactionStatus,
        long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy) {

    public static EmployeeAttendanceResponse from(EmployeeAttendance attendance) {
        return new EmployeeAttendanceResponse(
                attendance.getId(),
                attendance.getEmployee().getId(),
                attendance.getEmployee().getName(),
                attendance.getEmployee().isActive(),
                attendance.getBusinessDate(),
                attendance.getStatus(),
                attendance.getDailyWageAmount(),
                attendance.getDailyWageTransaction() == null ? null : attendance.getDailyWageTransaction().getId(),
                attendance.getDailyWageTransaction() == null
                        ? null
                        : attendance.getDailyWageTransaction().getStatus(),
                attendance.getVersion(),
                attendance.getCreatedAt(),
                attendance.getUpdatedAt(),
                attendance.getCreatedBy(),
                attendance.getUpdatedBy());
    }

    public static EmployeeAttendanceResponse notRecorded(Employee employee, LocalDate businessDate) {
        return new EmployeeAttendanceResponse(
                null,
                employee.getId(),
                employee.getName(),
                employee.isActive(),
                businessDate,
                null,
                employee.getDefaultDailyWage(),
                null,
                null,
                0,
                null,
                null,
                null,
                null);
    }
}

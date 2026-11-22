package com.omar.beautycenter.employee.dto;

import com.omar.beautycenter.employee.EmployeeAttendanceStatus;
import jakarta.validation.constraints.NotNull;

public record SaveEmployeeAttendanceRequest(
        @NotNull(message = "Attendance status is required.")
        EmployeeAttendanceStatus status) {
}

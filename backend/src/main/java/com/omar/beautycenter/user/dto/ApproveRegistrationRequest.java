package com.omar.beautycenter.user.dto;

import com.omar.beautycenter.role.Role;
import com.omar.beautycenter.user.EmployeeLinkMode;
import com.omar.beautycenter.user.SystemPermission;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Set;

public record ApproveRegistrationRequest(
        @NotNull(message = "Role is required.")
        Role role,
        Set<SystemPermission> permissions,
        EmployeeLinkMode employeeLinkMode,
        @Positive(message = "Employee id must be positive.") Long employeeId) {

    public ApproveRegistrationRequest(Role role) {
        this(role, null, null, null);
    }

    public ApproveRegistrationRequest(Role role, EmployeeLinkMode employeeLinkMode, Long employeeId) {
        this(role, null, employeeLinkMode, employeeId);
    }
}

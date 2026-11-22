package com.omar.beautycenter.user.dto;

import com.omar.beautycenter.role.Role;
import com.omar.beautycenter.user.EmployeeLinkMode;
import com.omar.beautycenter.user.SystemPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record CreateUserRequest(
        @NotBlank(message = "Username is required.")
        @Size(max = 100, message = "Username must not exceed 100 characters.")
        String username,
        @NotBlank(message = "Display name is required.")
        @Size(max = 150, message = "Display name must not exceed 150 characters.")
        String displayName,
        @NotBlank(message = "Password is required.")
        @Size(max = 200, message = "Password must not exceed 200 characters.")
        String password,
        @NotEmpty(message = "At least one role is required.")
        Set<@NotNull(message = "Role is required.") Role> roles,
        Set<SystemPermission> permissions,
        EmployeeLinkMode employeeLinkMode,
        @Positive(message = "Employee id must be positive.") Long employeeId) {

    public CreateUserRequest(String username, String displayName, String password, Set<Role> roles) {
        this(username, displayName, password, roles, null, null, null);
    }

    public CreateUserRequest(
            String username,
            String displayName,
            String password,
            Set<Role> roles,
            EmployeeLinkMode employeeLinkMode,
            Long employeeId) {
        this(username, displayName, password, roles, null, employeeLinkMode, employeeId);
    }
}

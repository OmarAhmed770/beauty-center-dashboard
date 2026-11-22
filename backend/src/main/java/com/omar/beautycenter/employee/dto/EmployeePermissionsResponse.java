package com.omar.beautycenter.employee.dto;

import com.omar.beautycenter.user.SystemPermission;
import com.omar.beautycenter.user.User;
import java.util.Set;

public record EmployeePermissionsResponse(
        Long employeeId,
        Long userId,
        String username,
        boolean accountLinked,
        Set<SystemPermission> permissions) {

    public static EmployeePermissionsResponse unlinked(Long employeeId) {
        return new EmployeePermissionsResponse(employeeId, null, null, false, Set.of());
    }

    public static EmployeePermissionsResponse from(Long employeeId, User user) {
        return new EmployeePermissionsResponse(
                employeeId, user.getId(), user.getUsername(), true, Set.copyOf(user.getPermissions()));
    }
}

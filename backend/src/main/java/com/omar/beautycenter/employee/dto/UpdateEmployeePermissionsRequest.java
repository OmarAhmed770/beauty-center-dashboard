package com.omar.beautycenter.employee.dto;

import com.omar.beautycenter.user.SystemPermission;
import java.util.Set;

public record UpdateEmployeePermissionsRequest(Set<SystemPermission> permissions) {
}

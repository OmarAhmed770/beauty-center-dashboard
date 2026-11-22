package com.omar.beautycenter.user.dto;

import com.omar.beautycenter.role.Role;
import com.omar.beautycenter.user.RegistrationStatus;
import com.omar.beautycenter.user.SystemPermission;
import com.omar.beautycenter.user.User;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String displayName,
        String phone,
        Long employeeId,
        String employeeName,
        boolean active,
        RegistrationStatus registrationStatus,
        Set<Role> roles,
        Set<SystemPermission> permissions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getPhone(),
                user.getEmployee() == null ? null : user.getEmployee().getId(),
                user.getEmployee() == null ? null : user.getEmployee().getName(),
                user.isActive(),
                user.getRegistrationStatus(),
                Set.copyOf(user.getRoles()),
                user.getRoles().contains(Role.OWNER)
                        ? Set.copyOf(EnumSet.allOf(SystemPermission.class))
                        : Set.copyOf(user.getPermissions()),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getCreatedBy(),
                user.getUpdatedBy());
    }
}

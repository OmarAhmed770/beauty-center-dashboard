package com.omar.beautycenter.user.dto;

import com.omar.beautycenter.role.Role;
import com.omar.beautycenter.user.SystemPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UpdateUserRequest(
        @NotBlank(message = "Display name is required.")
        @Size(max = 150, message = "Display name must not exceed 150 characters.")
        String displayName,
        @NotEmpty(message = "At least one role is required.")
        Set<@NotNull(message = "Role is required.") Role> roles,
        Set<SystemPermission> permissions) {

    public UpdateUserRequest(String displayName, Set<Role> roles) {
        this(displayName, roles, null);
    }
}

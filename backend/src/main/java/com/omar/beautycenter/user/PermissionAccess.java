package com.omar.beautycenter.user;

import com.omar.beautycenter.role.Role;
import java.util.Arrays;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Centralized server-side access check used by method security. */
@Component("permissionAccess")
public class PermissionAccess {

    public boolean has(String permissionName) {
        SystemPermission permission;
        try {
            permission = SystemPermission.valueOf(permissionName);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream().anyMatch(authority ->
                        authority.getAuthority().equals(Role.OWNER.authority())
                                || authority.getAuthority().equals(permission.authority()));
    }

    public boolean hasAny(String... permissionNames) {
        return Arrays.stream(permissionNames).anyMatch(this::has);
    }
}

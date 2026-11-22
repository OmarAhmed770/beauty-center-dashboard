package com.omar.beautycenter.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.omar.beautycenter.role.Role;
import com.omar.beautycenter.user.User;
import com.omar.beautycenter.user.RegistrationStatus;
import com.omar.beautycenter.user.SystemPermission;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String displayName;
    private final String passwordHash;
    private final boolean active;
    private final RegistrationStatus registrationStatus;
    private final Set<Role> roles;
    private final Set<SystemPermission> permissions;

    private UserPrincipal(
            Long id,
            String username,
            String displayName,
            String passwordHash,
            boolean active,
            RegistrationStatus registrationStatus,
            Set<Role> roles,
            Set<SystemPermission> permissions) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.active = active;
        this.registrationStatus = registrationStatus;
        this.roles = Set.copyOf(roles);
        this.permissions = Set.copyOf(permissions);
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getPasswordHash(),
                user.isActive(),
                user.getRegistrationStatus(),
                user.getRoles(),
                user.getPermissions());
    }

    public Long id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Set<Role> roles() {
        return roles;
    }

    public Set<SystemPermission> permissions() {
        return permissions;
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = java.util.stream.Stream.concat(
                        roles.stream().map(Role::authority),
                        permissions.stream().map(SystemPermission::authority))
                .sorted()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return List.copyOf(authorities);
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active && registrationStatus == RegistrationStatus.APPROVED;
    }
}

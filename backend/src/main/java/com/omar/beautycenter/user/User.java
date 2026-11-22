package com.omar.beautycenter.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.omar.beautycenter.common.audit.BaseAuditEntity;
import com.omar.beautycenter.employee.Employee;
import com.omar.beautycenter.role.Role;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.EnumSet;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "users")
public class User extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 100, unique = true)
    private String username;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "phone", length = 50)
    private String phone;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", unique = true)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false, length = 20)
    private RegistrationStatus registrationStatus = RegistrationStatus.APPROVED;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Set<Role> roles = EnumSet.noneOf(Role.class);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 50)
    private Set<SystemPermission> permissions = EnumSet.noneOf(SystemPermission.class);

    protected User() {
    }

    public User(
            String username,
            String displayName,
            String passwordHash,
            boolean active,
            Set<Role> roles) {
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.active = active;
        this.registrationStatus = RegistrationStatus.APPROVED;
        setRoles(roles);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public String getPhone() {
        return phone;
    }

    public Employee getEmployee() {
        return employee;
    }

    public RegistrationStatus getRegistrationStatus() {
        return registrationStatus;
    }

    public boolean isLockedAt(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public Set<SystemPermission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void changeDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void changePhone(String phone) {
        this.phone = phone;
    }

    public void linkEmployee(Employee employee) {
        this.employee = employee;
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void recordFailedLogin(
            LocalDateTime now,
            int maximumFailedAttempts,
            Duration lockDuration) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maximumFailedAttempts) {
            lockedUntil = now.plus(lockDuration);
            failedLoginAttempts = 0;
        }
    }

    public void clearLoginProtection() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    public void setRoles(Set<Role> roles) {
        this.roles.clear();
        if (roles == null) {
            return;
        }

        // V16 converts every persisted legacy ADMIN into a staff account with
        // explicit permissions. Keep the entity defensive as well so a legacy
        // caller cannot reintroduce an ADMIN row after that migration.
        if (roles.contains(Role.ADMIN)) {
            this.roles.add(Role.RECEPTIONIST);
            this.permissions.addAll(EnumSet.allOf(SystemPermission.class));
        }
        roles.stream()
                .filter(role -> role != Role.ADMIN)
                .forEach(this.roles::add);
        if (this.roles.contains(Role.RECEPTIONIST) && this.permissions.isEmpty()) {
            this.permissions.add(SystemPermission.ADD_ORDERS);
        }
    }

    public void setPermissions(Set<SystemPermission> permissions) {
        this.permissions.clear();
        if (permissions != null) {
            this.permissions.addAll(permissions);
        }
    }

    public void markPendingRegistration(String phone) {
        this.phone = phone;
        this.registrationStatus = RegistrationStatus.PENDING;
        this.active = false;
        this.roles.clear();
        this.permissions.clear();
    }

    public void approveRegistration(Set<Role> roles) {
        setRoles(roles);
        this.registrationStatus = RegistrationStatus.APPROVED;
        activate();
        clearLoginProtection();
    }

    public void rejectRegistration() {
        this.registrationStatus = RegistrationStatus.REJECTED;
        deactivate();
        clearLoginProtection();
    }
}

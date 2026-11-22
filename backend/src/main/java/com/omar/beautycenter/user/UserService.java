package com.omar.beautycenter.user;

import com.omar.beautycenter.audit.AuditLogService;
import com.omar.beautycenter.auth.dto.StaffRegistrationRequest;
import com.omar.beautycenter.auth.dto.StaffRegistrationResponse;
import com.omar.beautycenter.common.config.LoginProtectionProperties;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.common.error.NotFoundException;
import com.omar.beautycenter.employee.Employee;
import com.omar.beautycenter.employee.EmployeeRepository;
import com.omar.beautycenter.employee.dto.EmployeePermissionsResponse;
import com.omar.beautycenter.employee.dto.UpdateEmployeePermissionsRequest;
import com.omar.beautycenter.role.Role;
import com.omar.beautycenter.user.dto.UserResponse;
import com.omar.beautycenter.user.dto.ApproveRegistrationRequest;
import com.omar.beautycenter.user.dto.CreateUserRequest;
import com.omar.beautycenter.user.dto.ResetUserPasswordRequest;
import com.omar.beautycenter.user.dto.UpdateUserRequest;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("[a-z0-9._-]{3,100}");
    private static final int MINIMUM_PASSWORD_LENGTH = 12;
    private static final int BCRYPT_MAXIMUM_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginProtectionProperties loginProtectionProperties;
    private final Clock clock;
    private final AuditLogService auditLogService;

    public UserService(
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            LoginProtectionProperties loginProtectionProperties,
            Clock clock,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginProtectionProperties = loginProtectionProperties;
        this.clock = clock;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public User getEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found."));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAllByOrderByUsernameAsc().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('OWNER')")
    public List<UserResponse> listPendingRegistrations() {
        return userRepository.findByRegistrationStatusOrderByCreatedAtAsc(RegistrationStatus.PENDING).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('OWNER')")
    public EmployeePermissionsResponse employeePermissions(Long employeeId) {
        return userRepository.findByEmployeeId(employeeId)
                .map(user -> EmployeePermissionsResponse.from(employeeId, user))
                .orElseGet(() -> EmployeePermissionsResponse.unlinked(employeeId));
    }

    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public EmployeePermissionsResponse updateEmployeePermissions(
            Long employeeId,
            UpdateEmployeePermissionsRequest request) {
        User user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Link a staff account to this employee before assigning permissions."));
        if (user.getRoles().contains(Role.OWNER)) {
            throw new BusinessRuleException("OWNER permissions are fixed and cannot be changed.");
        }
        user.setPermissions(managedPermissions(request.permissions()));
        User saved = userRepository.saveAndFlush(user);
        auditLogService.record("EMPLOYEE_PERMISSIONS_UPDATED", "USER", saved.getId(), null,
                Map.of("employeeId", employeeId, "permissions", saved.getPermissions()));
        return EmployeePermissionsResponse.from(employeeId, saved);
    }

    @Transactional(readOnly = true)
    public boolean hasAnyUser() {
        return userRepository.count() > 0;
    }

    @Transactional
    public boolean isLoginAllowed(String username) {
        String normalizedUsername = normalizeLoginUsername(username);
        if (normalizedUsername == null) {
            return true;
        }
        return userRepository.findByUsernameForUpdate(normalizedUsername)
                .map(user -> !user.isLockedAt(LocalDateTime.now(clock)))
                .orElse(true);
    }

    @Transactional
    public void recordFailedLogin(String username) {
        String normalizedUsername = normalizeLoginUsername(username);
        if (normalizedUsername == null) {
            return;
        }
        userRepository.findByUsernameForUpdate(normalizedUsername)
                .filter(User::isActive)
                .ifPresent(user -> user.recordFailedLogin(
                        LocalDateTime.now(clock),
                        loginProtectionProperties.maximumFailedAttempts(),
                        loginProtectionProperties.lockDuration()));
    }

    @Transactional
    public void clearLoginProtection(Long userId) {
        userRepository.findById(userId).ifPresent(User::clearLoginProtection);
    }

    @Transactional
    public UserResponse createInitialOwner(
            String username,
            String displayName,
            String rawPassword) {
        if (userRepository.count() > 0) {
            throw new BusinessRuleException(
                "The initial OWNER can only be created when no users exist.");
        }

        String normalizedUsername = normalizeUsername(username);
        String normalizedDisplayName = validateDisplayName(displayName);
        validatePassword(rawPassword);

        User owner = new User(
                normalizedUsername,
                normalizedDisplayName,
                passwordEncoder.encode(rawPassword),
                true,
                Set.of(Role.OWNER));
        return UserResponse.from(userRepository.saveAndFlush(owner));
    }

    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public UserResponse create(CreateUserRequest request) {
        String username = normalizeUsername(request.username());
        if (userRepository.existsByUsername(username)) {
            throw new BusinessRuleException("A user with this username already exists.");
        }
        validatePassword(request.password());
        Set<Role> roles = validateManagedRoles(request.roles());
        User user = new User(
                username,
                validateDisplayName(request.displayName()),
                passwordEncoder.encode(request.password()),
                true,
                roles);
        user.setPermissions(managedPermissions(request.permissions()));
        linkStaffEmployee(user, roles, request.employeeLinkMode(), request.employeeId());
        UserResponse response = UserResponse.from(userRepository.saveAndFlush(user));
        auditLogService.record("USER_CREATED", "USER", user.getId(), null,
                Map.of("username", user.getUsername(), "roles", user.getRoles()));
        return response;
    }

    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public UserResponse update(Long userId, UpdateUserRequest request) {
        User user = getEntityById(userId);
        Set<Role> newRoles = validateUpdateRoles(user, request.roles());
        assertNotRemovingLastActiveOwner(user, user.isActive(), newRoles);
        user.changeDisplayName(validateDisplayName(request.displayName()));
        user.setRoles(newRoles);
        if (!newRoles.contains(Role.OWNER)) {
            user.setPermissions(managedPermissions(request.permissions()));
        }
        UserResponse response = UserResponse.from(userRepository.saveAndFlush(user));
        auditLogService.record("USER_UPDATED", "USER", user.getId(), null,
                Map.of("username", user.getUsername(), "roles", user.getRoles()));
        return response;
    }

    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public UserResponse setActive(Long userId, boolean active) {
        User user = getEntityById(userId);
        if (active && user.getRegistrationStatus() != RegistrationStatus.APPROVED) {
            throw new BusinessRuleException("Only approved accounts can be activated.");
        }
        assertNotRemovingLastActiveOwner(user, active, user.getRoles());
        if (active) {
            user.activate();
        } else {
            user.deactivate();
            user.clearLoginProtection();
        }
        UserResponse response = UserResponse.from(userRepository.saveAndFlush(user));
        auditLogService.record(active ? "USER_ACTIVATED" : "USER_DEACTIVATED", "USER", user.getId(), null,
                Map.of("username", user.getUsername()));
        return response;
    }

    @Transactional
    public StaffRegistrationResponse registerPendingStaff(StaffRegistrationRequest request) {
        if (!java.util.Objects.equals(request.password(), request.passwordConfirmation())) {
            throw new BusinessRuleException("Password confirmation does not match.");
        }
        String username = normalizeUsername(request.username());
        if (userRepository.existsByUsername(username)) {
            throw new BusinessRuleException("A user with this username already exists.");
        }
        validateRegistrationPassword(request.password());
        User user = new User(
                username,
                validateDisplayName(request.displayName()),
                passwordEncoder.encode(request.password()),
                false,
                Set.of());
        user.markPendingRegistration(optionalPhone(request.phone()));
        User saved = userRepository.saveAndFlush(user);
        auditLogService.record("STAFF_REGISTRATION_REQUESTED", "USER", saved.getId(), null,
                Map.of("username", saved.getUsername()));
        return new StaffRegistrationResponse(saved.getUsername(), saved.getRegistrationStatus());
    }

    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public UserResponse approveRegistration(Long userId, ApproveRegistrationRequest request) {
        User user = getEntityById(userId);
        if (user.getRegistrationStatus() != RegistrationStatus.PENDING) {
            throw new BusinessRuleException("Only pending registrations can be approved.");
        }
        Role role = validateRegistrationRole(request.role());
        user.approveRegistration(Set.of(role));
        user.setPermissions(managedPermissions(request.permissions()));
        linkStaffEmployee(user, Set.of(role), request.employeeLinkMode(), request.employeeId());
        UserResponse response = UserResponse.from(userRepository.saveAndFlush(user));
        auditLogService.record("STAFF_REGISTRATION_APPROVED", "USER", user.getId(), null,
                Map.of(
                        "username", user.getUsername(),
                        "role", role,
                        "employeeId", user.getEmployee() == null ? "UNLINKED" : user.getEmployee().getId()));
        return response;
    }

    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public UserResponse rejectRegistration(Long userId) {
        User user = getEntityById(userId);
        if (user.getRegistrationStatus() != RegistrationStatus.PENDING) {
            throw new BusinessRuleException("Only pending registrations can be rejected.");
        }
        user.rejectRegistration();
        UserResponse response = UserResponse.from(userRepository.saveAndFlush(user));
        auditLogService.record("STAFF_REGISTRATION_REJECTED", "USER", user.getId(), null,
                Map.of("username", user.getUsername()));
        return response;
    }

    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public UserResponse resetPassword(Long userId, ResetUserPasswordRequest request) {
        User user = getEntityById(userId);
        validatePassword(request.password());
        user.changePasswordHash(passwordEncoder.encode(request.password()));
        user.clearLoginProtection();
        UserResponse response = UserResponse.from(userRepository.saveAndFlush(user));
        auditLogService.record("USER_PASSWORD_RESET", "USER", user.getId(), null,
                Map.of("username", user.getUsername()));
        return response;
    }

    public static String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new BusinessRuleException(
                    "Username must contain 3 to 100 lowercase letters, numbers, dots, underscores, or hyphens.");
        }
        return normalized;
    }

    private static String validateDisplayName(String displayName) {
        String normalized = displayName == null ? "" : displayName.strip();
        if (normalized.isEmpty() || normalized.length() > 150) {
            throw new BusinessRuleException("Display name must contain 1 to 150 characters.");
        }
        return normalized;
    }

    private static void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new BusinessRuleException(
                    "Password must contain at least 12 characters.");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAXIMUM_PASSWORD_BYTES) {
            throw new BusinessRuleException(
                    "Password must not exceed 72 UTF-8 bytes.");
        }
    }

    private static void validateRegistrationPassword(String rawPassword) {
        validatePassword(rawPassword);
        boolean hasLetter = rawPassword.codePoints().anyMatch(Character::isLetter);
        boolean hasDigit = rawPassword.codePoints().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BusinessRuleException("Password must contain at least one letter and one number.");
        }
    }

    private static Set<Role> validateRoles(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new BusinessRuleException("At least one role is required.");
        }
        return EnumSet.copyOf(roles);
    }

    private static Set<Role> validateManagedRoles(Set<Role> roles) {
        Set<Role> validated = validateRoles(roles);
        if (!validated.equals(Set.of(Role.RECEPTIONIST))) {
            throw new BusinessRuleException("Ordinary accounts must be staff accounts.");
        }
        return validated;
    }

    private static Set<Role> validateUpdateRoles(User user, Set<Role> roles) {
        Set<Role> validated = validateRoles(roles);
        if (user.getRoles().contains(Role.OWNER)) {
            if (!validated.equals(Set.of(Role.OWNER))) {
                throw new BusinessRuleException("The single OWNER role cannot be changed through ordinary user management.");
            }
            return validated;
        }
        return validateManagedRoles(validated);
    }

    private static Role validateRegistrationRole(Role role) {
        if (role != Role.RECEPTIONIST) {
            throw new BusinessRuleException("A pending registration can only be approved as RECEPTIONIST.");
        }
        return role;
    }

    private static Set<SystemPermission> managedPermissions(Set<SystemPermission> permissions) {
        if (permissions == null) {
            return Set.of(SystemPermission.ADD_ORDERS);
        }
        return permissions.isEmpty()
                ? EnumSet.noneOf(SystemPermission.class)
                : EnumSet.copyOf(permissions);
    }

    private void linkStaffEmployee(
            User user,
            Set<Role> roles,
            EmployeeLinkMode requestedMode,
            Long requestedEmployeeId) {
        boolean isStaff = roles.contains(Role.RECEPTIONIST);
        EmployeeLinkMode mode = requestedMode == null
                ? (isStaff ? EmployeeLinkMode.AUTO_CREATE : EmployeeLinkMode.NONE)
                : requestedMode;

        if (!isStaff) {
            if (mode != EmployeeLinkMode.NONE || requestedEmployeeId != null) {
                throw new BusinessRuleException("Only staff accounts may be linked to an employee.");
            }
            return;
        }

        switch (mode) {
            case NONE -> {
                if (requestedEmployeeId != null) {
                    throw new BusinessRuleException("Employee id is only allowed when linking an existing employee.");
                }
                user.linkEmployee(null);
            }
            case EXISTING -> {
                if (requestedEmployeeId == null) {
                    throw new BusinessRuleException("Employee id is required when linking an existing employee.");
                }
                if (userRepository.existsByEmployeeId(requestedEmployeeId)) {
                    throw new BusinessRuleException("The selected employee is already linked to another user.");
                }
                Employee employee = employeeRepository.findById(requestedEmployeeId)
                        .orElseThrow(() -> new NotFoundException("Employee not found."));
                if (!employee.isActive()) {
                    throw new BusinessRuleException("Only active employees may be linked to a staff account.");
                }
                user.linkEmployee(employee);
            }
            case AUTO_CREATE -> {
                if (requestedEmployeeId != null) {
                    throw new BusinessRuleException("Employee id is not allowed when automatically creating an employee.");
                }
                Employee employee = new Employee(
                        user.getDisplayName(),
                        user.getPhone(),
                        "موظف",
                        new BigDecimal("0.00"),
                        "تم إنشاؤه تلقائيًا مع حساب الموظف.");
                user.linkEmployee(employeeRepository.saveAndFlush(employee));
            }
        }
    }

    private static String optionalPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String normalized = phone.strip();
        if (normalized.length() > 50) {
            throw new BusinessRuleException("Phone must not exceed 50 characters.");
        }
        return normalized;
    }

    private void assertNotRemovingLastActiveOwner(
            User user,
            boolean futureActive,
            Set<Role> futureRoles) {
        boolean isActiveOwner = user.isActive() && user.getRoles().contains(Role.OWNER);
        boolean remainsActiveOwner = futureActive && futureRoles.contains(Role.OWNER);
        if (isActiveOwner && !remainsActiveOwner
                && userRepository.countByActiveTrueAndRolesContaining(Role.OWNER) <= 1) {
            throw new BusinessRuleException("At least one active OWNER account must remain.");
        }
    }

    private static String normalizeLoginUsername(String username) {
        try {
            return normalizeUsername(username);
        } catch (BusinessRuleException exception) {
            return null;
        }
    }
}

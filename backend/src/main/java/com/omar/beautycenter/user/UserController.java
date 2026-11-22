package com.omar.beautycenter.user;

import com.omar.beautycenter.user.dto.CreateUserRequest;
import com.omar.beautycenter.user.dto.ApproveRegistrationRequest;
import com.omar.beautycenter.user.dto.ResetUserPasswordRequest;
import com.omar.beautycenter.user.dto.SetUserActiveRequest;
import com.omar.beautycenter.user.dto.UpdateUserRequest;
import com.omar.beautycenter.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "List application users", description = "OWNER role only")
    public List<UserResponse> listUsers() {
        return userService.listUsers();
    }

    @GetMapping("/pending-registrations")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "List pending staff registration requests", description = "OWNER role only")
    public List<UserResponse> listPendingRegistrations() {
        return userService.listPendingRegistrations();
    }

    @PatchMapping("/{userId}/registration/approve")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Approve a pending staff registration", description = "OWNER role only; assigns RECEPTIONIST")
    public UserResponse approveRegistration(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody ApproveRegistrationRequest request) {
        return userService.approveRegistration(userId, request);
    }

    @PatchMapping("/{userId}/registration/reject")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Reject a pending staff registration", description = "OWNER role only")
    public UserResponse rejectRegistration(@PathVariable @Positive Long userId) {
        return userService.rejectRegistration(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Create an application user", description = "OWNER role only")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Update application user details and roles", description = "OWNER role only")
    public UserResponse update(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(userId, request);
    }

    @PatchMapping("/{userId}/active")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Activate or deactivate an application user", description = "OWNER role only")
    public UserResponse setActive(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody SetUserActiveRequest request) {
        return userService.setActive(userId, request.active());
    }

    @PostMapping("/{userId}/password-reset")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Reset an application user password", description = "OWNER role only; no password data is returned")
    public UserResponse resetPassword(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody ResetUserPasswordRequest request) {
        return userService.resetPassword(userId, request);
    }
}

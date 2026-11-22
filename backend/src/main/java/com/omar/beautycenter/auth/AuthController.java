package com.omar.beautycenter.auth;

import com.omar.beautycenter.auth.dto.AccessTokenResponse;
import com.omar.beautycenter.auth.dto.LoginRequest;
import com.omar.beautycenter.auth.dto.StaffRegistrationRequest;
import com.omar.beautycenter.auth.dto.StaffRegistrationResponse;
import com.omar.beautycenter.common.config.OpenApiConfiguration;
import com.omar.beautycenter.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final StaffRegistrationRateLimiter registrationRateLimiter;

    public AuthController(
            AuthService authService,
            StaffRegistrationRateLimiter registrationRateLimiter) {
        this.authService = authService;
        this.registrationRateLimiter = registrationRateLimiter;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Log in with username and password")
    public AccessTokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Submit a local staff registration request", description = "Creates a pending account without a role or access")
    public StaffRegistrationResponse register(
            @Valid @RequestBody StaffRegistrationRequest request,
            HttpServletRequest httpRequest) {
        registrationRateLimiter.recordAttempt(httpRequest.getRemoteAddr());
        return authService.registerStaff(request);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH_SCHEME)
    @Operation(summary = "Get the currently authenticated user")
    public UserResponse currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return authService.currentUser(principal);
    }
}

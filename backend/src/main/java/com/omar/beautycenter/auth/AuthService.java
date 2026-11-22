package com.omar.beautycenter.auth;

import com.omar.beautycenter.audit.AuditLogService;
import com.omar.beautycenter.auth.JwtService.GeneratedAccessToken;
import com.omar.beautycenter.auth.dto.AccessTokenResponse;
import com.omar.beautycenter.auth.dto.LoginRequest;
import com.omar.beautycenter.auth.dto.StaffRegistrationRequest;
import com.omar.beautycenter.auth.dto.StaffRegistrationResponse;
import com.omar.beautycenter.user.UserService;
import com.omar.beautycenter.user.dto.UserResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserService userService,
            AuditLogService auditLogService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    public AccessTokenResponse login(LoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length
                > BCRYPT_MAX_PASSWORD_BYTES) {
            BadCredentialsException exception = new BadCredentialsException(
                    "Invalid username or password.");
            recordFailedLogin(request.username(), "PASSWORD_TOO_LONG");
            throw exception;
        }
        if (!userService.isLoginAllowed(request.username())) {
            BadCredentialsException exception = new BadCredentialsException(
                    "Invalid username or password.");
            recordFailedLogin(request.username(), "TEMPORARILY_LOCKED");
            throw exception;
        }
        final org.springframework.security.core.Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.username(), request.password()));
        } catch (AuthenticationException exception) {
            recordFailedLogin(request.username(), exception.getClass().getSimpleName());
            throw exception;
        }
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        userService.clearLoginProtection(principal.id());
        GeneratedAccessToken token = jwtService.generateAccessToken(principal);
        UserResponse user = userService.getById(principal.id());
        auditLogService.record("LOGIN_SUCCESS", "USER", principal.id(), null,
                Map.of("username", principal.getUsername(), "roles", principal.roles()));
        return new AccessTokenResponse(
                token.value(),
                "Bearer",
                token.expiresInSeconds(),
                token.expiresAt(),
                user);
    }

    public UserResponse currentUser(UserPrincipal principal) {
        return userService.getById(principal.id());
    }

    public StaffRegistrationResponse registerStaff(StaffRegistrationRequest request) {
        return userService.registerPendingStaff(request);
    }

    private void recordFailedLogin(String username, String failureType) {
        userService.recordFailedLogin(username);
        auditLogService.record("LOGIN_FAILED", "USER", username, null,
                Map.of("username", username, "failureType", failureType));
    }
}

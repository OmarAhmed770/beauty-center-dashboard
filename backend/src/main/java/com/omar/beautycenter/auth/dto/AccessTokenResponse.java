package com.omar.beautycenter.auth.dto;

import com.omar.beautycenter.user.dto.UserResponse;
import java.time.Instant;

public record AccessTokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        Instant expiresAt,
        UserResponse user) {
}

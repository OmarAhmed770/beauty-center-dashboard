package com.omar.beautycenter.common.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.registration-protection")
public record RegistrationProtectionProperties(
        int maximumAttempts,
        Duration window) {

    public RegistrationProtectionProperties {
        if (maximumAttempts < 1) {
            throw new IllegalArgumentException("maximumAttempts must be at least 1.");
        }
        if (window == null || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("window must be positive.");
        }
    }
}

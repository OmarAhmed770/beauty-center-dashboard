package com.omar.beautycenter.common.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.login-protection")
public record LoginProtectionProperties(
        int maximumFailedAttempts,
        Duration lockDuration) {

    public LoginProtectionProperties {
        if (maximumFailedAttempts < 1) {
            throw new IllegalArgumentException("maximumFailedAttempts must be at least 1.");
        }
        if (lockDuration == null || lockDuration.isNegative() || lockDuration.isZero()) {
            throw new IllegalArgumentException("lockDuration must be positive.");
        }
    }
}

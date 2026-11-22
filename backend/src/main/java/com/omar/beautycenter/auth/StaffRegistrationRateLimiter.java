package com.omar.beautycenter.auth;

import com.omar.beautycenter.common.config.RegistrationProtectionProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class StaffRegistrationRateLimiter {

    private final RegistrationProtectionProperties properties;
    private final Clock clock;
    private final Map<String, AttemptWindow> attemptsByClient = new ConcurrentHashMap<>();

    public StaffRegistrationRateLimiter(
            RegistrationProtectionProperties properties,
            Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public synchronized void recordAttempt(String clientAddress) {
        Instant now = clock.instant();
        attemptsByClient.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        String key = clientAddress == null || clientAddress.isBlank() ? "unknown" : clientAddress;
        AttemptWindow current = attemptsByClient.get(key);
        if (current == null || !current.expiresAt().isAfter(now)) {
            attemptsByClient.put(key, new AttemptWindow(1, now.plus(properties.window())));
            return;
        }
        if (current.attempts() >= properties.maximumAttempts()) {
            throw new RegistrationRateLimitException();
        }
        attemptsByClient.put(key, new AttemptWindow(current.attempts() + 1, current.expiresAt()));
    }

    public void clear() {
        attemptsByClient.clear();
    }

    private record AttemptWindow(int attempts, Instant expiresAt) {
    }
}

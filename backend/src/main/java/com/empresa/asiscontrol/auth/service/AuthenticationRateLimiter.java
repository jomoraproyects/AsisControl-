package com.empresa.asiscontrol.auth.service;

import com.empresa.asiscontrol.shared.config.SecurityProperties;
import com.empresa.asiscontrol.shared.exception.RateLimitException;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationRateLimiter {

    private final ConcurrentHashMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();
    private final SecurityProperties properties;
    private final Clock clock;

    public AuthenticationRateLimiter(SecurityProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void check(String key) {
        Instant now = clock.instant();
        AttemptWindow window = attempts.get(key);
        if (window != null && now.isBefore(window.startedAt.plus(properties.loginAttemptWindow()))
                && window.count >= properties.loginAttemptLimit()) {
            throw new RateLimitException();
        }
    }

    public void failure(String key) {
        Instant now = clock.instant();
        attempts.compute(key, (ignored, current) -> {
            if (current == null || !now.isBefore(current.startedAt.plus(properties.loginAttemptWindow()))) {
                return new AttemptWindow(now, 1);
            }
            return new AttemptWindow(current.startedAt, current.count + 1);
        });
    }

    public void success(String key) {
        attempts.remove(key);
    }

    private record AttemptWindow(Instant startedAt, int count) {
    }
}


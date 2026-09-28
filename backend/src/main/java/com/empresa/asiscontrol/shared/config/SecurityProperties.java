package com.empresa.asiscontrol.shared.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("asiscontrol.security")
public record SecurityProperties(
        String mfaEncryptionKey,
        List<String> frontendOrigins,
        Duration pendingAuthenticationTtl,
        int loginAttemptLimit,
        Duration loginAttemptWindow) {
}


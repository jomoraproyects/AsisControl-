package com.empresa.asiscontrol.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("asiscontrol.recovery")
public record RecoveryProperties(
        boolean enabled,
        String username,
        String passwordFile,
        boolean resetMfa) {
}


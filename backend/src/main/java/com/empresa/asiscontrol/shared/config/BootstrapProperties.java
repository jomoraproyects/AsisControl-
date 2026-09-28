package com.empresa.asiscontrol.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("asiscontrol.bootstrap")
public record BootstrapProperties(
        boolean enabled,
        String username,
        String email,
        String passwordFile) {
}


package com.empresa.asiscontrol.auth.service;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

public record PendingAuthentication(
        Long userId,
        String username,
        long authVersion,
        Instant expiresAt) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}


package com.empresa.asiscontrol.auth.dto;

import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import java.util.Set;
import java.util.UUID;

public record SessionResponse(
        UUID userId,
        String username,
        Set<String> roles,
        Set<String> permissions,
        boolean passwordChangeRequired) {

    public static SessionResponse from(AsisUserPrincipal principal) {
        Set<String> permissions = principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> !authority.startsWith("ROLE_"))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new SessionResponse(principal.publicId(), principal.getUsername(), principal.roles(), permissions,
                principal.mustChangePassword());
    }
}


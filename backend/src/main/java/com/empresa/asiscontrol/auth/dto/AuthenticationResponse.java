package com.empresa.asiscontrol.auth.dto;

import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import java.util.Set;
import java.util.UUID;

public record AuthenticationResponse(
        String status,
        UUID userId,
        String username,
        Set<String> roles) {

    public static AuthenticationResponse pending(String status, AsisUserPrincipal principal) {
        return new AuthenticationResponse(status, principal.publicId(), principal.getUsername(), principal.roles());
    }

    public static AuthenticationResponse authenticated(AsisUserPrincipal principal) {
        return new AuthenticationResponse("AUTHENTICATED", principal.publicId(), principal.getUsername(),
                principal.roles());
    }
}


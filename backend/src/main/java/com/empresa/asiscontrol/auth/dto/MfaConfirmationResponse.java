package com.empresa.asiscontrol.auth.dto;

import java.util.List;

public record MfaConfirmationResponse(
        AuthenticationResponse authentication,
        List<String> recoveryCodes) {
}


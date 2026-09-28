package com.empresa.asiscontrol.auth.dto;

import jakarta.validation.constraints.Size;

public record MfaVerificationRequest(
        @Size(max = 20) String totpCode,
        @Size(max = 30) String recoveryCode) {
}


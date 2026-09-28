package com.empresa.asiscontrol.auth.dto;

public record MfaEnrollmentResponse(String secretBase32, String otpauthUri) {
}


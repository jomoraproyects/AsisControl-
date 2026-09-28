package com.empresa.asiscontrol.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaCodeRequest(
        @NotBlank @Pattern(regexp = "\\d{6}", message = "debe contener seis dígitos") String code) {
}


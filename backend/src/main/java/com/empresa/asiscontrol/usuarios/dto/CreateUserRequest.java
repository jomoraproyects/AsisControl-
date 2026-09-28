package com.empresa.asiscontrol.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 80) String username,
        @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 200) String password,
        @NotEmpty Set<@NotBlank String> roles,
        Long empleadoId) {
}

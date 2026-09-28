package com.empresa.asiscontrol.usuarios.dto;

import com.empresa.asiscontrol.usuarios.entity.Usuario;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        boolean active,
        boolean passwordChangeRequired,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt) {

    public static UserResponse from(Usuario user, Set<String> roles) {
        return new UserResponse(user.getPublicId(), user.getNombreUsuario(), user.getCorreo(), user.isActivo(),
                user.isDebeCambiarPassword(), Set.copyOf(roles), user.getCreadoEn(), user.getActualizadoEn());
    }
}


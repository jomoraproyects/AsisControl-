package com.empresa.asiscontrol.roles.dto;

import com.empresa.asiscontrol.roles.entity.Rol;
import java.util.Set;
import java.util.stream.Collectors;

public record RoleResponse(
        String code,
        String name,
        boolean protectedRole,
        boolean mfaRequired,
        boolean assignableInCurrentPhase,
        Set<String> permissions) {

    public static RoleResponse from(Rol role, boolean assignable) {
        return new RoleResponse(role.getCodigo(), role.getNombre(), role.isProtegido(), role.isRequiereMfa(),
                assignable, role.getPermisos().stream().map(permission -> permission.getCodigo())
                        .collect(Collectors.toUnmodifiableSet()));
    }
}


package com.empresa.asiscontrol.cargos.dto;

import com.empresa.asiscontrol.cargos.entity.Cargo;
import com.empresa.asiscontrol.shared.domain.EstadoRegistro;
import java.time.Instant;

public record CargoResponse(Long id, String codigo, String nombre, String descripcion,
                            EstadoRegistro estado, Instant createdAt, Instant updatedAt, long version) {
    public static CargoResponse from(Cargo cargo) {
        return new CargoResponse(cargo.getId(), cargo.getCodigo(), cargo.getNombre(), cargo.getDescripcion(),
                cargo.getEstado(), cargo.getCreadoEn(), cargo.getActualizadoEn(), cargo.getVersion());
    }
}

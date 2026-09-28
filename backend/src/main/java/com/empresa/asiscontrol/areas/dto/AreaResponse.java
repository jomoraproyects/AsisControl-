package com.empresa.asiscontrol.areas.dto;

import com.empresa.asiscontrol.areas.entity.Area;
import com.empresa.asiscontrol.shared.domain.EstadoRegistro;
import java.time.Instant;

public record AreaResponse(Long id, String codigo, String nombre, String descripcion,
                           EstadoRegistro estado, Instant createdAt, Instant updatedAt, long version) {
    public static AreaResponse from(Area area) {
        return new AreaResponse(area.getId(), area.getCodigo(), area.getNombre(), area.getDescripcion(),
                area.getEstado(), area.getCreadoEn(), area.getActualizadoEn(), area.getVersion());
    }
}

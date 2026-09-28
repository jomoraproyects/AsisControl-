package com.empresa.asiscontrol.cuadrillas.dto;

import com.empresa.asiscontrol.cuadrillas.entity.Cuadrilla;
import com.empresa.asiscontrol.cuadrillas.entity.EstadoCuadrilla;
import com.empresa.asiscontrol.empleados.dto.EmpleadoResumenResponse;
import java.time.Instant;
import java.util.List;

public record CuadrillaResponse(Long id, String codigo, String nombre, String descripcion,
        EstadoCuadrilla estado, Long supervisorId, List<EmpleadoResumenResponse> empleados,
        Instant createdAt, Instant updatedAt, long version) {
    public static CuadrillaResponse from(Cuadrilla c, Long supervisorId, List<EmpleadoResumenResponse> members) {
        return new CuadrillaResponse(c.getId(), c.getCodigo(), c.getNombre(), c.getDescripcion(),
                c.getEstado(), supervisorId, members, c.getCreadoEn(), c.getActualizadoEn(), c.getVersion());
    }
}

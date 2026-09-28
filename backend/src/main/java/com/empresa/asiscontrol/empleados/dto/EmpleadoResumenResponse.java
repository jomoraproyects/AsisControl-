package com.empresa.asiscontrol.empleados.dto;

import com.empresa.asiscontrol.empleados.entity.Empleado;
import com.empresa.asiscontrol.empleados.entity.TipoEmpleado;
import com.empresa.asiscontrol.shared.domain.EstadoRegistro;

public record EmpleadoResumenResponse(Long id, String nombreCompleto, String codigoEmpleado,
        TipoEmpleado tipoEmpleado, EstadoRegistro estado) {
    public static EmpleadoResumenResponse from(Empleado e) {
        return new EmpleadoResumenResponse(e.getId(), e.getNombreCompleto(), e.getCodigoEmpleado(),
                e.getTipoEmpleado(), e.getEstado());
    }
}

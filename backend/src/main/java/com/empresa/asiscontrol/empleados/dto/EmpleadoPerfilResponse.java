package com.empresa.asiscontrol.empleados.dto;

import com.empresa.asiscontrol.empleados.entity.Empleado;
import com.empresa.asiscontrol.empleados.entity.TipoEmpleado;

public record EmpleadoPerfilResponse(Long id, String nombreCompleto, String codigoEmpleado, TipoEmpleado tipoEmpleado) {
    public static EmpleadoPerfilResponse from(Empleado e) {
        return new EmpleadoPerfilResponse(e.getId(), e.getNombreCompleto(), e.getCodigoEmpleado(), e.getTipoEmpleado());
    }
}

package com.empresa.asiscontrol.empleados.dto;

import com.empresa.asiscontrol.empleados.entity.*;
import com.empresa.asiscontrol.shared.domain.EstadoRegistro;
import java.time.Instant;
import java.time.LocalDate;

public record EmpleadoResponse(Long id, TipoDocumento tipoDocumento, String numeroDocumentoNormalizado,
        String nombres, String apellidos, String nombreCompleto, String codigoEmpleado,
        Long areaId, Long cargoId, TipoEmpleado tipoEmpleado, EstadoRegistro estado,
        LocalDate fechaIngreso, LocalDate fechaInactivacion, String motivoInactivacion,
        Instant createdAt, Instant updatedAt, long version) {
    public static EmpleadoResponse from(Empleado e) {
        return new EmpleadoResponse(e.getId(), e.getTipoDocumento(), e.getNumeroDocumentoNormalizado(),
                e.getNombres(), e.getApellidos(), e.getNombreCompleto(), e.getCodigoEmpleado(),
                e.getAreaId(), e.getCargoId(), e.getTipoEmpleado(), e.getEstado(),
                e.getFechaIngreso(), e.getFechaInactivacion(), e.getMotivoInactivacion(),
                e.getCreadoEn(), e.getActualizadoEn(), e.getVersion());
    }
}

package com.empresa.asiscontrol.supervisores.dto;

import com.empresa.asiscontrol.supervisores.entity.SupervisorEmpleado;
import java.time.LocalDate;

public record SupervisorAssignmentResponse(Long id, Long supervisorId, Long empleadoId,
        LocalDate vigenteDesde, LocalDate vigenteHasta) {
    public static SupervisorAssignmentResponse from(SupervisorEmpleado row) {
        return new SupervisorAssignmentResponse(row.getId(), row.getSupervisorId(), row.getEmpleadoId(),
                row.getVigenteDesde(), row.getVigenteHasta());
    }
}

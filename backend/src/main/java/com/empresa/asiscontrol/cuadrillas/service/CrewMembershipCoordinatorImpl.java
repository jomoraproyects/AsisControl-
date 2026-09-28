package com.empresa.asiscontrol.cuadrillas.service;

import com.empresa.asiscontrol.auditoria.entity.AuditAction;
import com.empresa.asiscontrol.auditoria.service.AuditService;
import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import com.empresa.asiscontrol.cuadrillas.entity.CuadrillaEmpleado;
import com.empresa.asiscontrol.cuadrillas.repository.*;
import com.empresa.asiscontrol.shared.exception.ConflictException;
import com.empresa.asiscontrol.shared.web.RequestMetadata;
import com.empresa.asiscontrol.supervisores.service.CrewMembershipCoordinator;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrewMembershipCoordinatorImpl implements CrewMembershipCoordinator {
    private final CuadrillaEmpleadoRepository members;
    private final CuadrillaSupervisorRepository leaders;
    private final CuadrillaRepository crews;
    private final AuditService audit;
    public CrewMembershipCoordinatorImpl(CuadrillaEmpleadoRepository members,
            CuadrillaSupervisorRepository leaders, CuadrillaRepository crews, AuditService audit) {
        this.members = members; this.leaders = leaders; this.crews = crews; this.audit = audit;
    }
    @Override @Transactional
    public void closeIfIncompatible(Long employeeId, Long newSupervisorId, LocalDate date,
            AsisUserPrincipal actor, RequestMetadata metadata) {
        CuadrillaEmpleado row = members.findByEmpleadoIdAndVigenteHastaIsNull(employeeId).orElse(null);
        if (row == null) return;
        Long crewSupervisor = leaders.activeOn(row.getCuadrillaId(), date)
                .map(value -> value.getSupervisorId()).orElse(null);
        if (newSupervisorId != null && newSupervisorId.equals(crewSupervisor)) return;
        crews.lockById(row.getCuadrillaId()).orElseThrow();
        if (!date.isAfter(row.getVigenteDesde()))
            throw new ConflictException("CAMBIO_MISMO_DIA", "La pertenencia a cuadrilla debe completar un día");
        row.cerrar(date);
        audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.RETIRAR_EMPLEADO_CUADRILLA,
                "CUADRILLA_EMPLEADO", row.getId().toString(), null,
                Map.of("vigenteHasta", date.toString()), Map.of("reason", "SUPERVISOR_CHANGE"), metadata);
    }
}

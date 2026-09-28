package com.empresa.asiscontrol.supervisores.service;

import com.empresa.asiscontrol.auditoria.entity.AuditAction;
import com.empresa.asiscontrol.auditoria.service.AuditService;
import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import com.empresa.asiscontrol.empleados.entity.TipoEmpleado;
import com.empresa.asiscontrol.empleados.service.EmployeeAccess;
import com.empresa.asiscontrol.shared.exception.ConflictException;
import com.empresa.asiscontrol.shared.exception.NotFoundException;
import com.empresa.asiscontrol.shared.web.RequestMetadata;
import com.empresa.asiscontrol.supervisores.dto.*;
import com.empresa.asiscontrol.supervisores.entity.SupervisorEmpleado;
import com.empresa.asiscontrol.supervisores.repository.SupervisorEmpleadoRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupervisorAssignmentService {
    private final SupervisorEmpleadoRepository repository;
    private final EmployeeAccess employees;
    private final CrewMembershipCoordinator crew;
    private final AuditService audit;
    private final Clock clock;
    public SupervisorAssignmentService(SupervisorEmpleadoRepository repository, EmployeeAccess employees,
            CrewMembershipCoordinator crew, AuditService audit, Clock clock) {
        this.repository = repository; this.employees = employees; this.crew = crew;
        this.audit = audit; this.clock = clock;
    }
    @Transactional
    public SupervisorAssignmentResponse assign(Long employeeId, AsignarSupervisorRequest input,
            AsisUserPrincipal actor, RequestMetadata metadata) {
        LocalDate date = input.vigenteDesde();
        requirePresentOrPast(date);
        if (employeeId.equals(input.supervisorId()))
            throw new ConflictException("SUPERVISOR_PROPIO", "El empleado no puede supervisarse a sí mismo");
        employees.lockActive(employeeId);
        employees.requireActiveType(input.supervisorId(), TipoEmpleado.SUPERVISOR);
        SupervisorEmpleado previous = repository.findByEmpleadoIdAndVigenteHastaIsNull(employeeId).orElse(null);
        if (previous != null) {
            if (previous.getSupervisorId().equals(input.supervisorId()))
                throw new ConflictException("SUPERVISOR_YA_ASIGNADO", "El supervisor ya está vigente");
            if (!date.isAfter(previous.getVigenteDesde()))
                throw new ConflictException("CAMBIO_MISMO_DIA", "La vigencia actual debe completar un día");
            crew.closeIfIncompatible(employeeId, input.supervisorId(), date, actor, metadata);
            previous.cerrar(date, input.motivoCambio());
            repository.flush();
            audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.CERRAR_ASIGNACION_SUPERVISOR,
                    "SUPERVISOR_EMPLEADO", previous.getId().toString(), null,
                    Map.of("vigenteHasta", date.toString()), Map.of(), metadata);
        }
        if (!repository.overlappingFrom(employeeId, date).isEmpty())
            throw new ConflictException("SUPERVISOR_SOLAPADO", "Existe una asignación que se solapa");
        SupervisorEmpleado row = repository.save(SupervisorEmpleado.crear(input.supervisorId(), employeeId,
                date, actor.userId(), input.motivoCambio(), clock.instant()));
        audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.ASIGNAR_SUPERVISOR,
                "SUPERVISOR_EMPLEADO", row.getId().toString(), null,
                Map.of("empleadoId", employeeId, "supervisorId", input.supervisorId(),
                        "vigenteDesde", date.toString()), Map.of(), metadata);
        return SupervisorAssignmentResponse.from(row);
    }
    @Transactional
    public void close(Long employeeId, CerrarSupervisorRequest input, AsisUserPrincipal actor,
                      RequestMetadata metadata) {
        requirePresentOrPast(input.vigenteHasta());
        employees.lockActive(employeeId);
        SupervisorEmpleado row = repository.findByEmpleadoIdAndVigenteHastaIsNull(employeeId)
                .orElseThrow(() -> new NotFoundException("SUPERVISOR_NO_ASIGNADO", "Sin supervisor vigente"));
        if (!input.vigenteHasta().isAfter(row.getVigenteDesde()))
            throw new ConflictException("CAMBIO_MISMO_DIA", "La vigencia actual debe completar un día");
        crew.closeIfIncompatible(employeeId, null, input.vigenteHasta(), actor, metadata);
        row.cerrar(input.vigenteHasta(), input.motivoCambio());
        audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.CERRAR_ASIGNACION_SUPERVISOR,
                "SUPERVISOR_EMPLEADO", row.getId().toString(), null,
                Map.of("vigenteHasta", input.vigenteHasta().toString()), Map.of(), metadata);
    }
    @Transactional
    public void closeForInactive(Long employeeId, LocalDate date, AsisUserPrincipal actor, RequestMetadata metadata) {
        repository.findByEmpleadoIdAndVigenteHastaIsNull(employeeId).ifPresent(row -> {
            if (!date.isAfter(row.getVigenteDesde()))
                throw new ConflictException("CAMBIO_MISMO_DIA", "La asignación debe completar un día");
            crew.closeIfIncompatible(employeeId, null, date, actor, metadata);
            row.cerrar(date, "Inactivación de empleado");
            audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.CERRAR_ASIGNACION_SUPERVISOR,
                    "SUPERVISOR_EMPLEADO", row.getId().toString(), null,
                    Map.of("vigenteHasta", date.toString()), Map.of("reason", "EMPLOYEE_DEACTIVATION"), metadata);
        });
    }
    private void requirePresentOrPast(LocalDate date) {
        if (date.isAfter(LocalDate.now(clock)))
            throw new ConflictException("FECHA_FUTURA", "La fecha efectiva futura no está habilitada en el MVP");
    }
}

package com.empresa.asiscontrol.cuadrillas.service;

import com.empresa.asiscontrol.auditoria.entity.AuditAction;
import com.empresa.asiscontrol.auditoria.service.AuditService;
import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import com.empresa.asiscontrol.cuadrillas.dto.*;
import com.empresa.asiscontrol.cuadrillas.entity.*;
import com.empresa.asiscontrol.cuadrillas.repository.*;
import com.empresa.asiscontrol.empleados.entity.TipoEmpleado;
import com.empresa.asiscontrol.empleados.service.EmployeeAccess;
import com.empresa.asiscontrol.shared.exception.ConflictException;
import com.empresa.asiscontrol.shared.exception.ForbiddenOperationException;
import com.empresa.asiscontrol.shared.exception.NotFoundException;
import com.empresa.asiscontrol.shared.web.RequestMetadata;
import com.empresa.asiscontrol.supervisores.service.SupervisorScopeService;
import com.empresa.asiscontrol.usuarios.service.UserLinkService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrewService {
    private final CuadrillaRepository crews;
    private final CuadrillaSupervisorRepository leaders;
    private final CuadrillaEmpleadoRepository members;
    private final EmployeeAccess employees;
    private final SupervisorScopeService scope;
    private final UserLinkService links;
    private final AuditService audit;
    private final Clock clock;
    public CrewService(CuadrillaRepository crews, CuadrillaSupervisorRepository leaders,
            CuadrillaEmpleadoRepository members, EmployeeAccess employees, SupervisorScopeService scope,
            UserLinkService links, AuditService audit, Clock clock) {
        this.crews = crews; this.leaders = leaders; this.members = members; this.employees = employees;
        this.scope = scope; this.links = links; this.audit = audit; this.clock = clock;
    }
    @Transactional(readOnly = true)
    public List<CuadrillaResponse> list(AsisUserPrincipal actor) {
        if (actor.roles().contains("SUPER_ADMIN")) return crews.findAll().stream().map(this::response).toList();
        Long employeeId = links.requireEmployeeId(actor.userId());
        return leaders.crewsLedOn(employeeId, LocalDate.now(clock)).stream()
                .map(id -> response(require(id))).toList();
    }
    @Transactional(readOnly = true)
    public CuadrillaResponse get(Long id, AsisUserPrincipal actor) {
        if (!actor.roles().contains("SUPER_ADMIN")) {
            Long own = links.requireEmployeeId(actor.userId());
            if (!leaders.crewsLedOn(own, LocalDate.now(clock)).contains(id))
                throw new NotFoundException("CUADRILLA_NO_EXISTE", "Cuadrilla no encontrada");
        }
        return response(require(id));
    }
    @Transactional
    public CuadrillaResponse create(CreateCuadrillaRequest input, AsisUserPrincipal actor, RequestMetadata metadata) {
        String code = input.codigo().trim().toUpperCase(Locale.ROOT);
        if (crews.existsByCodigo(code)) throw new ConflictException("CUADRILLA_DUPLICADA", "Código de cuadrilla registrado");
        Cuadrilla row = crews.save(Cuadrilla.crear(code, input.nombre().trim(), input.descripcion(), clock.instant()));
        audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.CREAR_CUADRILLA, "CUADRILLA",
                row.getId().toString(), null, Map.of("codigo", code), Map.of(), metadata);
        return response(row);
    }
    @Transactional
    public CuadrillaResponse update(Long id, UpdateCuadrillaRequest input, AsisUserPrincipal actor, RequestMetadata metadata) {
        Cuadrilla row = require(id);
        String before = row.getNombre();
        row.actualizar(input.nombre().trim(), input.descripcion(), clock.instant());
        audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.MODIFICAR_CUADRILLA, "CUADRILLA",
                id.toString(), Map.of("nombre", before), Map.of("nombre", row.getNombre()), Map.of(), metadata);
        return response(row);
    }
    @Transactional
    public void deactivate(Long id, AsisUserPrincipal actor, RequestMetadata metadata) {
        Cuadrilla row = lockActive(id);
        LocalDate today = LocalDate.now(clock);
        if (!members.activeMembers(id, today).isEmpty())
            throw new ConflictException("CUADRILLA_CON_EMPLEADOS", "Retire los empleados antes de desactivar");
        if (leaders.findByCuadrillaIdAndVigenteHastaIsNull(id).isPresent())
            throw new ConflictException("CUADRILLA_CON_SUPERVISOR", "Cierre el supervisor antes de desactivar");
        row.desactivar(clock.instant());
        audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.MODIFICAR_CUADRILLA, "CUADRILLA",
                id.toString(), Map.of("estado", "ACTIVA"), Map.of("estado", "INACTIVA"), Map.of(), metadata);
    }
    @Transactional
    public CuadrillaResponse assignSupervisor(Long id, AsignarSupervisorCuadrillaRequest input,
            AsisUserPrincipal actor, RequestMetadata metadata) {
        LocalDate date = input.vigenteDesde(); requirePresentOrPast(date);
        lockActive(id);
        employees.requireActiveType(input.supervisorId(), TipoEmpleado.SUPERVISOR);
        CuadrillaSupervisor previous = leaders.findByCuadrillaIdAndVigenteHastaIsNull(id).orElse(null);
        if (previous != null) {
            if (previous.getSupervisorId().equals(input.supervisorId()))
                throw new ConflictException("SUPERVISOR_YA_ASIGNADO", "Supervisor ya vigente");
            if (!date.isAfter(previous.getVigenteDesde()))
                throw new ConflictException("CAMBIO_MISMO_DIA", "La vigencia debe completar un día");
        }
        for (CuadrillaEmpleado member : members.activeMembers(id, date)) {
            if (!scope.assignedOn(input.supervisorId(), member.getEmpleadoId(), date))
                throw new ConflictException("CUADRILLA_SUPERVISOR_INCOMPATIBLE",
                        "Los empleados deben estar asignados al supervisor nuevo");
        }
        if (previous != null) { previous.cerrar(date); leaders.flush(); }
        if (!leaders.overlappingFrom(id, date).isEmpty())
            throw new ConflictException("SUPERVISOR_CUADRILLA_SOLAPADO", "Vigencia solapada");
        CuadrillaSupervisor row = leaders.save(CuadrillaSupervisor.crear(id, input.supervisorId(), date,
                actor.userId(), clock.instant()));
        audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.ASIGNAR_SUPERVISOR_CUADRILLA,
                "CUADRILLA_SUPERVISOR", row.getId().toString(), null,
                Map.of("cuadrillaId", id, "supervisorId", input.supervisorId(), "vigenteDesde", date.toString()),
                Map.of(), metadata);
        return response(require(id));
    }
    @Transactional
    public void closeSupervisor(Long id, CerrarVigenciaRequest input, AsisUserPrincipal actor, RequestMetadata metadata) {
        LocalDate date = input.vigenteHasta(); requirePresentOrPast(date);
        lockActive(id);
        if (!members.activeMembers(id, date).isEmpty())
            throw new ConflictException("CUADRILLA_CON_EMPLEADOS", "Retire los empleados antes de cerrar supervisor");
        CuadrillaSupervisor row = leaders.findByCuadrillaIdAndVigenteHastaIsNull(id)
                .orElseThrow(() -> new NotFoundException("SUPERVISOR_NO_ASIGNADO", "Sin supervisor vigente"));
        if (!date.isAfter(row.getVigenteDesde()))
            throw new ConflictException("CAMBIO_MISMO_DIA", "La vigencia debe completar un día");
        row.cerrar(date);
        audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.ASIGNAR_SUPERVISOR_CUADRILLA,
                "CUADRILLA_SUPERVISOR", row.getId().toString(), null,
                Map.of("vigenteHasta", date.toString()), Map.of("operation", "CLOSE"), metadata);
    }
    @Transactional
    public CuadrillaResponse addEmployee(Long id, AgregarEmpleadoCuadrillaRequest input,
            AsisUserPrincipal actor, RequestMetadata metadata) {
        LocalDate date = input.vigenteDesde(); requirePresentOrPast(date);
        employees.lockActive(input.empleadoId());
        lockActive(id);
        CuadrillaSupervisor leader = leaders.activeOn(id, date)
                .orElseThrow(() -> new ConflictException("CUADRILLA_SIN_SUPERVISOR", "Cuadrilla sin supervisor vigente"));
        if (!scope.assignedOn(leader.getSupervisorId(), input.empleadoId(), date))
            throw new ConflictException("CUADRILLA_SUPERVISOR_INCOMPATIBLE",
                    "El empleado no está asignado al supervisor de la cuadrilla");
        if (!members.overlappingFrom(input.empleadoId(), date).isEmpty())
            throw new ConflictException("EMPLEADO_CUADRILLA_SOLAPADO", "El empleado ya tiene cuadrilla vigente");
        CuadrillaEmpleado row = members.save(CuadrillaEmpleado.crear(id, input.empleadoId(), date,
                actor.userId(), clock.instant()));
        audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.AGREGAR_EMPLEADO_CUADRILLA,
                "CUADRILLA_EMPLEADO", row.getId().toString(), null,
                Map.of("cuadrillaId", id, "empleadoId", input.empleadoId(), "vigenteDesde", date.toString()),
                Map.of(), metadata);
        return response(require(id));
    }
    @Transactional
    public void removeEmployee(Long id, Long employeeId, CerrarVigenciaRequest input,
            AsisUserPrincipal actor, RequestMetadata metadata) {
        LocalDate date = input.vigenteHasta(); requirePresentOrPast(date);
        employees.lockActive(employeeId);
        lockActive(id);
        CuadrillaEmpleado row = members.findByEmpleadoIdAndVigenteHastaIsNull(employeeId)
                .filter(value -> value.getCuadrillaId().equals(id))
                .orElseThrow(() -> new NotFoundException("EMPLEADO_NO_EN_CUADRILLA", "Empleado no asignado"));
        if (!date.isAfter(row.getVigenteDesde()))
            throw new ConflictException("CAMBIO_MISMO_DIA", "La vigencia debe completar un día");
        row.cerrar(date);
        audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.RETIRAR_EMPLEADO_CUADRILLA,
                "CUADRILLA_EMPLEADO", row.getId().toString(), null,
                Map.of("vigenteHasta", date.toString()), Map.of(), metadata);
    }
    private CuadrillaResponse response(Cuadrilla c) {
        LocalDate today = LocalDate.now(clock);
        Long leaderId = leaders.activeOn(c.getId(), today).map(CuadrillaSupervisor::getSupervisorId).orElse(null);
        List<Long> ids = members.activeMembers(c.getId(), today).stream().map(CuadrillaEmpleado::getEmpleadoId).toList();
        return CuadrillaResponse.from(c, leaderId, employees.summaries(ids));
    }
    private Cuadrilla require(Long id) {
        return crews.findById(id).orElseThrow(() -> new NotFoundException("CUADRILLA_NO_EXISTE", "Cuadrilla no encontrada"));
    }
    private Cuadrilla lockActive(Long id) {
        Cuadrilla c = crews.lockById(id).orElseThrow(() ->
                new NotFoundException("CUADRILLA_NO_EXISTE", "Cuadrilla no encontrada"));
        if (c.getEstado() != EstadoCuadrilla.ACTIVA)
            throw new ConflictException("CUADRILLA_INACTIVA", "Cuadrilla inactiva");
        return c;
    }
    private void requirePresentOrPast(LocalDate date) {
        if (date.isAfter(LocalDate.now(clock)))
            throw new ConflictException("FECHA_FUTURA", "La fecha futura no está habilitada en el MVP");
    }
}

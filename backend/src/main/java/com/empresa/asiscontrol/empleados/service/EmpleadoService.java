package com.empresa.asiscontrol.empleados.service;

import com.empresa.asiscontrol.areas.service.AreaService;
import com.empresa.asiscontrol.auditoria.entity.AuditAction;
import com.empresa.asiscontrol.auditoria.service.AuditService;
import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import com.empresa.asiscontrol.cargos.service.CargoService;
import com.empresa.asiscontrol.cuadrillas.service.CrewLeadershipService;
import com.empresa.asiscontrol.empleados.dto.*;
import com.empresa.asiscontrol.empleados.entity.*;
import com.empresa.asiscontrol.empleados.repository.EmpleadoRepository;
import com.empresa.asiscontrol.shared.domain.EstadoRegistro;
import com.empresa.asiscontrol.shared.exception.ConflictException;
import com.empresa.asiscontrol.shared.exception.NotFoundException;
import com.empresa.asiscontrol.shared.web.RequestMetadata;
import com.empresa.asiscontrol.supervisores.service.SupervisorAssignmentService;
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
public class EmpleadoService {
    private final EmpleadoRepository repository;
    private final AreaService areas;
    private final CargoService cargos;
    private final SupervisorScopeService scope;
    private final SupervisorAssignmentService assignments;
    private final CrewLeadershipService leadership;
    private final UserLinkService links;
    private final AuditService audit;
    private final Clock clock;
    public EmpleadoService(EmpleadoRepository repository, AreaService areas, CargoService cargos,
            SupervisorScopeService scope, SupervisorAssignmentService assignments,
            CrewLeadershipService leadership, UserLinkService links, AuditService audit, Clock clock) {
        this.repository = repository; this.areas = areas; this.cargos = cargos; this.scope = scope;
        this.assignments = assignments; this.leadership = leadership; this.links = links;
        this.audit = audit; this.clock = clock;
    }
    @Transactional(readOnly = true)
    public List<EmpleadoResumenResponse> list(AsisUserPrincipal actor) {
        if (canSeeAll(actor)) return repository.findAll().stream().map(EmpleadoResumenResponse::from).toList();
        Long supervisorId = links.requireEmployeeId(actor.userId());
        return repository.findByIdIn(scope.assignedIds(supervisorId)).stream()
                .filter(e -> e.getEstado() == EstadoRegistro.ACTIVO)
                .map(EmpleadoResumenResponse::from).toList();
    }
    @Transactional(readOnly = true)
    public EmpleadoResponse get(Long id, AsisUserPrincipal actor) {
        if (!canSeeAll(actor)) {
            Long supervisorId = links.requireEmployeeId(actor.userId());
            if (!scope.canSee(supervisorId, id))
                throw new NotFoundException("EMPLEADO_NO_EXISTE", "Empleado no encontrado");
        }
        return EmpleadoResponse.from(require(id));
    }
    @Transactional(readOnly = true)
    public EmpleadoPerfilResponse ownProfile(AsisUserPrincipal actor) {
        return EmpleadoPerfilResponse.from(require(links.requireEmployeeId(actor.userId())));
    }
    @Transactional
    public EmpleadoResponse create(CreateEmpleadoRequest input, AsisUserPrincipal actor, RequestMetadata metadata) {
        String doc = DocumentNormalizer.normalize(input.numeroDocumento());
        String code = input.codigoEmpleado().trim().toUpperCase(Locale.ROOT);
        if (repository.existsByTipoDocumentoAndNumeroDocumentoNormalizado(input.tipoDocumento(), doc))
            throw new ConflictException("DOCUMENTO_DUPLICADO", "Documento ya registrado");
        if (repository.existsByCodigoEmpleado(code))
            throw new ConflictException("CODIGO_EMPLEADO_DUPLICADO", "Código de empleado ya registrado");
        areas.requireActive(input.areaId()); cargos.requireActive(input.cargoId());
        if (input.fechaIngreso().isAfter(LocalDate.now(clock)))
            throw new ConflictException("FECHA_INGRESO_FUTURA", "Fecha de ingreso futura no habilitada");
        Empleado row = repository.save(Empleado.crear(input.tipoDocumento(), doc, input.nombres().trim(),
                input.apellidos().trim(), code, input.areaId(), input.cargoId(), input.tipoEmpleado(),
                input.fechaIngreso(), clock.instant()));
        audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.CREAR_EMPLEADO, "EMPLEADO",
                row.getId().toString(), null,
                Map.of("codigo", code, "tipo", input.tipoEmpleado().name()), Map.of(), metadata);
        return EmpleadoResponse.from(row);
    }
    @Transactional
    public EmpleadoResponse update(Long id, UpdateEmpleadoRequest input, AsisUserPrincipal actor, RequestMetadata metadata) {
        Empleado row = require(id);
        if (row.getEstado() != EstadoRegistro.ACTIVO)
            throw new ConflictException("EMPLEADO_INACTIVO", "Empleado inactivo");
        areas.requireActive(input.areaId()); cargos.requireActive(input.cargoId());
        String before = row.getNombreCompleto();
        row.actualizar(input.nombres().trim(), input.apellidos().trim(), input.areaId(), input.cargoId(), clock.instant());
        audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.MODIFICAR_EMPLEADO, "EMPLEADO",
                id.toString(), Map.of("nombre", before), Map.of("nombre", row.getNombreCompleto()), Map.of(), metadata);
        return EmpleadoResponse.from(row);
    }
    @Transactional
    public void deactivate(Long id, DesactivarEmpleadoRequest input, AsisUserPrincipal actor, RequestMetadata metadata) {
        Empleado row = repository.lockById(id).orElseThrow(() ->
                new NotFoundException("EMPLEADO_NO_EXISTE", "Empleado no encontrado"));
        if (row.getEstado() == EstadoRegistro.INACTIVO) return;
        LocalDate today = LocalDate.now(clock);
        LocalDate date = input.fechaInactivacion() == null ? today : input.fechaInactivacion();
        if (!date.equals(today))
            throw new ConflictException("FECHA_INACTIVACION", "La inactivación efectiva es hoy");
        if (row.getTipoEmpleado() == TipoEmpleado.SUPERVISOR &&
                (!scope.assignedIds(id).isEmpty() || leadership.leadsAny(id)))
            throw new ConflictException("SUPERVISOR_CON_ASIGNACIONES", "Reasigne empleados y cuadrillas primero");
        assignments.closeForInactive(id, date, actor, metadata);
        links.deactivateLinked(id, actor, metadata);
        row.desactivar(date, input.motivo().trim(), clock.instant());
        audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.DESACTIVAR_EMPLEADO, "EMPLEADO",
                id.toString(), Map.of("estado", "ACTIVO"), Map.of("estado", "INACTIVO"),
                Map.of("fecha", date.toString()), metadata);
    }
    private boolean canSeeAll(AsisUserPrincipal actor) {
        return actor.roles().contains("SUPER_ADMIN") || actor.roles().contains("RRHH");
    }
    private Empleado require(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new NotFoundException("EMPLEADO_NO_EXISTE", "Empleado no encontrado"));
    }
}

package com.empresa.asiscontrol.supervisores.controller;

import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import com.empresa.asiscontrol.empleados.dto.EmpleadoResumenResponse;
import com.empresa.asiscontrol.empleados.service.EmployeeAccess;
import com.empresa.asiscontrol.roles.Permisos;
import com.empresa.asiscontrol.shared.exception.NotFoundException;
import com.empresa.asiscontrol.shared.web.RequestMetadataProvider;
import com.empresa.asiscontrol.supervisores.dto.*;
import com.empresa.asiscontrol.supervisores.service.*;
import com.empresa.asiscontrol.usuarios.service.UserLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class SupervisorController {
    private final SupervisorAssignmentService assignments;
    private final SupervisorScopeService scope;
    private final EmployeeAccess employees;
    private final UserLinkService links;
    private final RequestMetadataProvider metadata;
    public SupervisorController(SupervisorAssignmentService assignments, SupervisorScopeService scope,
            EmployeeAccess employees, UserLinkService links, RequestMetadataProvider metadata) {
        this.assignments = assignments; this.scope = scope; this.employees = employees;
        this.links = links; this.metadata = metadata;
    }
    @GetMapping("/supervisores")
    @PreAuthorize("hasAuthority('" + Permisos.SUPERVISOR_ASIGNAR + "')")
    public List<EmpleadoResumenResponse> list() { return employees.activeSupervisors(); }
    @GetMapping("/supervisores/{id}/empleados")
    @PreAuthorize("hasAnyAuthority('EMPLEADO_VER_TODOS','EMPLEADO_VER_ASIGNADOS')")
    public List<EmpleadoResumenResponse> assigned(@PathVariable Long id,
            @AuthenticationPrincipal AsisUserPrincipal actor) {
        if (!actor.roles().contains("SUPER_ADMIN") && !actor.roles().contains("RRHH")
                && !id.equals(links.requireEmployeeId(actor.userId())))
            throw new NotFoundException("SUPERVISOR_NO_EXISTE", "Supervisor no encontrado");
        return employees.summaries(scope.assignedIds(id));
    }
    @PostMapping("/empleados/{empleadoId}/supervisor")
    @PreAuthorize("hasAuthority('" + Permisos.SUPERVISOR_ASIGNAR + "')")
    public SupervisorAssignmentResponse assign(@PathVariable Long empleadoId,
            @Valid @RequestBody AsignarSupervisorRequest input,
            @AuthenticationPrincipal AsisUserPrincipal actor, HttpServletRequest request) {
        return assignments.assign(empleadoId, input, actor, metadata.from(request));
    }
    @PostMapping("/empleados/{empleadoId}/supervisor/cerrar")
    @PreAuthorize("hasAuthority('" + Permisos.SUPERVISOR_ASIGNAR + "')")
    public ResponseEntity<Void> close(@PathVariable Long empleadoId,
            @Valid @RequestBody CerrarSupervisorRequest input,
            @AuthenticationPrincipal AsisUserPrincipal actor, HttpServletRequest request) {
        assignments.close(empleadoId, input, actor, metadata.from(request));
        return ResponseEntity.noContent().build();
    }
}

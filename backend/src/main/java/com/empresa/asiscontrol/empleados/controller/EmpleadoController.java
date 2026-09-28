package com.empresa.asiscontrol.empleados.controller;

import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import com.empresa.asiscontrol.empleados.dto.*;
import com.empresa.asiscontrol.empleados.service.EmpleadoService;
import com.empresa.asiscontrol.roles.Permisos;
import com.empresa.asiscontrol.shared.web.RequestMetadataProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/empleados")
public class EmpleadoController {
    private final EmpleadoService service;
    private final RequestMetadataProvider metadata;
    public EmpleadoController(EmpleadoService service, RequestMetadataProvider metadata) {
        this.service = service; this.metadata = metadata;
    }
    @GetMapping
    @PreAuthorize("hasAnyAuthority('EMPLEADO_VER_TODOS','EMPLEADO_VER_ASIGNADOS')")
    public List<EmpleadoResumenResponse> list(@AuthenticationPrincipal AsisUserPrincipal actor) {
        return service.list(actor);
    }
    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('EMPLEADO_VER_PROPIO','EMPLEADO_VER_ASIGNADOS')")
    public EmpleadoPerfilResponse me(@AuthenticationPrincipal AsisUserPrincipal actor) {
        return service.ownProfile(actor);
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('EMPLEADO_VER_TODOS','EMPLEADO_VER_ASIGNADOS')")
    public EmpleadoResponse get(@PathVariable Long id, @AuthenticationPrincipal AsisUserPrincipal actor) {
        return service.get(id, actor);
    }
    @PostMapping
    @PreAuthorize("hasAuthority('" + Permisos.EMPLEADO_CREAR + "')")
    public ResponseEntity<EmpleadoResponse> create(@Valid @RequestBody CreateEmpleadoRequest input,
            @AuthenticationPrincipal AsisUserPrincipal actor, HttpServletRequest request) {
        EmpleadoResponse result = service.create(input, actor, metadata.from(request));
        return ResponseEntity.created(URI.create("/api/v1/empleados/" + result.id())).body(result);
    }
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.EMPLEADO_MODIFICAR + "')")
    public EmpleadoResponse update(@PathVariable Long id, @Valid @RequestBody UpdateEmpleadoRequest input,
            @AuthenticationPrincipal AsisUserPrincipal actor, HttpServletRequest request) {
        return service.update(id, input, actor, metadata.from(request));
    }
    @PostMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('" + Permisos.EMPLEADO_DESACTIVAR + "')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id,
            @Valid @RequestBody DesactivarEmpleadoRequest input,
            @AuthenticationPrincipal AsisUserPrincipal actor, HttpServletRequest request) {
        service.deactivate(id, input, actor, metadata.from(request));
        return ResponseEntity.noContent().build();
    }
}

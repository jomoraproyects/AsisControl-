package com.empresa.asiscontrol.cuadrillas.controller;

import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import com.empresa.asiscontrol.cuadrillas.dto.*;
import com.empresa.asiscontrol.cuadrillas.service.CrewService;
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
@RequestMapping("/api/v1/cuadrillas")
public class CuadrillaController {
    private final CrewService service;
    private final RequestMetadataProvider metadata;
    public CuadrillaController(CrewService service, RequestMetadataProvider metadata) {
        this.service = service; this.metadata = metadata;
    }
    @GetMapping
    @PreAuthorize("hasAnyAuthority('CUADRILLA_GESTIONAR','CUADRILLA_VER_PROPIA')")
    public List<CuadrillaResponse> list(@AuthenticationPrincipal AsisUserPrincipal actor) {
        return service.list(actor);
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CUADRILLA_GESTIONAR','CUADRILLA_VER_PROPIA')")
    public CuadrillaResponse get(@PathVariable Long id, @AuthenticationPrincipal AsisUserPrincipal actor) {
        return service.get(id, actor);
    }
    @PostMapping
    @PreAuthorize("hasAuthority('" + Permisos.CUADRILLA_GESTIONAR + "')")
    public ResponseEntity<CuadrillaResponse> create(@Valid @RequestBody CreateCuadrillaRequest input,
            @AuthenticationPrincipal AsisUserPrincipal actor, HttpServletRequest request) {
        CuadrillaResponse result = service.create(input, actor, metadata.from(request));
        return ResponseEntity.created(URI.create("/api/v1/cuadrillas/" + result.id())).body(result);
    }
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.CUADRILLA_GESTIONAR + "')")
    public CuadrillaResponse update(@PathVariable Long id, @Valid @RequestBody UpdateCuadrillaRequest input,
            @AuthenticationPrincipal AsisUserPrincipal actor, HttpServletRequest request) {
        return service.update(id, input, actor, metadata.from(request));
    }
    @PostMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('" + Permisos.CUADRILLA_GESTIONAR + "')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id,
            @AuthenticationPrincipal AsisUserPrincipal actor, HttpServletRequest request) {
        service.deactivate(id, actor, metadata.from(request));
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/supervisor")
    @PreAuthorize("hasAuthority('" + Permisos.CUADRILLA_GESTIONAR + "')")
    public CuadrillaResponse assignSupervisor(@PathVariable Long id,
            @Valid @RequestBody AsignarSupervisorCuadrillaRequest input,
            @AuthenticationPrincipal AsisUserPrincipal actor, HttpServletRequest request) {
        return service.assignSupervisor(id, input, actor, metadata.from(request));
    }
    @PostMapping("/{id}/supervisor/cerrar")
    @PreAuthorize("hasAuthority('" + Permisos.CUADRILLA_GESTIONAR + "')")
    public ResponseEntity<Void> closeSupervisor(@PathVariable Long id,
            @Valid @RequestBody CerrarVigenciaRequest input,
            @AuthenticationPrincipal AsisUserPrincipal actor, HttpServletRequest request) {
        service.closeSupervisor(id, input, actor, metadata.from(request));
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/empleados")
    @PreAuthorize("hasAuthority('" + Permisos.CUADRILLA_GESTIONAR + "')")
    public CuadrillaResponse addEmployee(@PathVariable Long id,
            @Valid @RequestBody AgregarEmpleadoCuadrillaRequest input,
            @AuthenticationPrincipal AsisUserPrincipal actor, HttpServletRequest request) {
        return service.addEmployee(id, input, actor, metadata.from(request));
    }
    @PostMapping("/{id}/empleados/{empleadoId}/retirar")
    @PreAuthorize("hasAuthority('" + Permisos.CUADRILLA_GESTIONAR + "')")
    public ResponseEntity<Void> removeEmployee(@PathVariable Long id, @PathVariable Long empleadoId,
            @Valid @RequestBody CerrarVigenciaRequest input,
            @AuthenticationPrincipal AsisUserPrincipal actor, HttpServletRequest request) {
        service.removeEmployee(id, empleadoId, input, actor, metadata.from(request));
        return ResponseEntity.noContent().build();
    }
}

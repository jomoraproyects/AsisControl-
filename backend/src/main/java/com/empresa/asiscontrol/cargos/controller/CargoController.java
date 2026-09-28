package com.empresa.asiscontrol.cargos.controller;

import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import com.empresa.asiscontrol.cargos.dto.*;
import com.empresa.asiscontrol.cargos.service.CargoService;
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
@RequestMapping("/api/v1/cargos")
public class CargoController {
    private final CargoService service;
    private final RequestMetadataProvider metadata;
    public CargoController(CargoService service, RequestMetadataProvider metadata) {
        this.service = service; this.metadata = metadata;
    }
    @GetMapping
    @PreAuthorize("hasAnyAuthority('CARGO_GESTIONAR','EMPLEADO_VER_TODOS')")
    public List<CargoResponse> list() { return service.list(); }
    @PostMapping
    @PreAuthorize("hasAuthority('" + Permisos.CARGO_GESTIONAR + "')")
    public ResponseEntity<CargoResponse> create(@Valid @RequestBody CargoRequest input,
            @AuthenticationPrincipal AsisUserPrincipal actor, HttpServletRequest request) {
        CargoResponse result = service.create(input, actor, metadata.from(request));
        return ResponseEntity.created(URI.create("/api/v1/cargos/" + result.id())).body(result);
    }
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.CARGO_GESTIONAR + "')")
    public CargoResponse update(@PathVariable Long id, @Valid @RequestBody UpdateCargoRequest input,
            @AuthenticationPrincipal AsisUserPrincipal actor, HttpServletRequest request) {
        return service.update(id, input, actor, metadata.from(request));
    }
    @PostMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('" + Permisos.CARGO_GESTIONAR + "')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id,
            @AuthenticationPrincipal AsisUserPrincipal actor, HttpServletRequest request) {
        service.deactivate(id, actor, metadata.from(request));
        return ResponseEntity.noContent().build();
    }
}

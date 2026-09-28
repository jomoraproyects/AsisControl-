package com.empresa.asiscontrol.areas.controller;

import com.empresa.asiscontrol.areas.dto.*;
import com.empresa.asiscontrol.areas.service.AreaService;
import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
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
@RequestMapping("/api/v1/areas")
public class AreaController {
    private final AreaService service;
    private final RequestMetadataProvider metadata;
    public AreaController(AreaService service, RequestMetadataProvider metadata) {
        this.service = service; this.metadata = metadata;
    }
    @GetMapping
    @PreAuthorize("hasAnyAuthority('AREA_GESTIONAR','EMPLEADO_VER_TODOS')")
    public List<AreaResponse> list() { return service.list(); }
    @PostMapping
    @PreAuthorize("hasAuthority('" + Permisos.AREA_GESTIONAR + "')")
    public ResponseEntity<AreaResponse> create(@Valid @RequestBody AreaRequest input,
            @AuthenticationPrincipal AsisUserPrincipal actor, HttpServletRequest request) {
        AreaResponse result = service.create(input, actor, metadata.from(request));
        return ResponseEntity.created(URI.create("/api/v1/areas/" + result.id())).body(result);
    }
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.AREA_GESTIONAR + "')")
    public AreaResponse update(@PathVariable Long id, @Valid @RequestBody UpdateAreaRequest input,
            @AuthenticationPrincipal AsisUserPrincipal actor, HttpServletRequest request) {
        return service.update(id, input, actor, metadata.from(request));
    }
    @PostMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('" + Permisos.AREA_GESTIONAR + "')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id,
            @AuthenticationPrincipal AsisUserPrincipal actor, HttpServletRequest request) {
        service.deactivate(id, actor, metadata.from(request));
        return ResponseEntity.noContent().build();
    }
}

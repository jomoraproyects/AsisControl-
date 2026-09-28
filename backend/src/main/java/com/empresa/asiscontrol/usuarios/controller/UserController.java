package com.empresa.asiscontrol.usuarios.controller;

import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import com.empresa.asiscontrol.roles.Permisos;
import com.empresa.asiscontrol.shared.web.RequestMetadataProvider;
import com.empresa.asiscontrol.usuarios.dto.CreateUserRequest;
import com.empresa.asiscontrol.usuarios.dto.LinkEmployeeRequest;
import com.empresa.asiscontrol.usuarios.dto.TemporaryPasswordResponse;
import com.empresa.asiscontrol.usuarios.dto.UpdateUserRequest;
import com.empresa.asiscontrol.usuarios.dto.UserResponse;
import com.empresa.asiscontrol.usuarios.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UserController {

    private final UserService service;
    private final RequestMetadataProvider metadataProvider;

    public UserController(UserService service, RequestMetadataProvider metadataProvider) {
        this.service = service;
        this.metadataProvider = metadataProvider;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.USUARIO_MODIFICAR + "')")
    public List<UserResponse> list() {
        return service.list();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permisos.USUARIO_CREAR + "')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest input,
                                               @AuthenticationPrincipal AsisUserPrincipal actor,
                                               HttpServletRequest request) {
        UserResponse created = service.create(input, actor, metadataProvider.from(request));
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + created.id())).body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.USUARIO_MODIFICAR + "')")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest input,
                               @AuthenticationPrincipal AsisUserPrincipal actor,
                               HttpServletRequest request) {
        return service.update(id, input, actor, metadataProvider.from(request));
    }

    @PostMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('" + Permisos.USUARIO_DESACTIVAR + "')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id,
                                           @AuthenticationPrincipal AsisUserPrincipal actor,
                                           HttpServletRequest request) {
        service.deactivate(id, actor, metadataProvider.from(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/roles/{roleCode}")
    @PreAuthorize("hasAuthority('" + Permisos.ROL_ASIGNAR + "')")
    public UserResponse assignRole(@PathVariable UUID id, @PathVariable String roleCode,
                                   @AuthenticationPrincipal AsisUserPrincipal actor,
                                   HttpServletRequest request) {
        return service.assignRole(id, roleCode, actor, metadataProvider.from(request));
    }

    @DeleteMapping("/{id}/roles/{roleCode}")
    @PreAuthorize("hasAuthority('" + Permisos.ROL_ASIGNAR + "')")
    public UserResponse revokeRole(@PathVariable UUID id, @PathVariable String roleCode,
                                   @AuthenticationPrincipal AsisUserPrincipal actor,
                                   HttpServletRequest request) {
        return service.revokeRole(id, roleCode, actor, metadataProvider.from(request));
    }

    @PostMapping("/{id}/empleado")
    @PreAuthorize("hasAuthority('" + Permisos.USUARIO_MODIFICAR + "')")
    public UserResponse linkEmployee(@PathVariable UUID id, @Valid @RequestBody LinkEmployeeRequest input,
            @AuthenticationPrincipal AsisUserPrincipal actor, HttpServletRequest request) {
        return service.linkEmployee(id, input.empleadoId(), actor, metadataProvider.from(request));
    }

    @DeleteMapping("/{id}/empleado")
    @PreAuthorize("hasAuthority('" + Permisos.USUARIO_MODIFICAR + "')")
    public UserResponse unlinkEmployee(@PathVariable UUID id,
            @AuthenticationPrincipal AsisUserPrincipal actor, HttpServletRequest request) {
        return service.unlinkEmployee(id, actor, metadataProvider.from(request));
    }

    @PostMapping("/{id}/restablecer-password")
    @PreAuthorize("hasAuthority('" + Permisos.USUARIO_MODIFICAR + "')")
    public TemporaryPasswordResponse resetPassword(@PathVariable UUID id,
                                                    @AuthenticationPrincipal AsisUserPrincipal actor,
                                                    HttpServletRequest request) {
        return new TemporaryPasswordResponse(service.resetPassword(id, actor, metadataProvider.from(request)));
    }
}

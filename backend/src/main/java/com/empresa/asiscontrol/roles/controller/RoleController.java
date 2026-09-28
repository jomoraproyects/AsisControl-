package com.empresa.asiscontrol.roles.controller;

import com.empresa.asiscontrol.roles.Permisos;
import com.empresa.asiscontrol.roles.dto.RoleResponse;
import com.empresa.asiscontrol.roles.service.RoleCatalogService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleCatalogService service;

    public RoleController(RoleCatalogService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.ROL_ASIGNAR + "')")
    public List<RoleResponse> list() {
        return service.list();
    }
}


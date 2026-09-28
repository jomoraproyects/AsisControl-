package com.empresa.asiscontrol.auditoria.controller;

import com.empresa.asiscontrol.auditoria.dto.AuditEventResponse;
import com.empresa.asiscontrol.auditoria.service.AuditQueryService;
import com.empresa.asiscontrol.roles.Permisos;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auditoria")
public class AuditController {

    private final AuditQueryService service;

    public AuditController(AuditQueryService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permisos.AUDITORIA_VER_TOTAL + "')")
    public Page<AuditEventResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.list(page, size);
    }
}

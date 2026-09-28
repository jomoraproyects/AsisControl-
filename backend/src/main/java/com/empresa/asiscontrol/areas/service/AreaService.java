package com.empresa.asiscontrol.areas.service;

import com.empresa.asiscontrol.areas.dto.*;
import com.empresa.asiscontrol.areas.entity.Area;
import com.empresa.asiscontrol.areas.repository.AreaRepository;
import com.empresa.asiscontrol.auditoria.entity.AuditAction;
import com.empresa.asiscontrol.auditoria.service.AuditService;
import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import com.empresa.asiscontrol.shared.domain.EstadoRegistro;
import com.empresa.asiscontrol.shared.exception.ConflictException;
import com.empresa.asiscontrol.shared.exception.NotFoundException;
import com.empresa.asiscontrol.shared.web.RequestMetadata;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AreaService {
    private final AreaRepository repository;
    private final AuditService audit;
    private final Clock clock;
    public AreaService(AreaRepository repository, AuditService audit, Clock clock) {
        this.repository = repository; this.audit = audit; this.clock = clock;
    }
    @Transactional(readOnly = true)
    public List<AreaResponse> list() { return repository.findAll().stream().map(AreaResponse::from).toList(); }
    @Transactional(readOnly = true)
    public void requireActive(Long id) {
        if (get(id).estado() != EstadoRegistro.ACTIVO) {
            throw new ConflictException("AREA_INACTIVA", "El área está inactiva");
        }
    }
    @Transactional(readOnly = true)
    public AreaResponse get(Long id) { return AreaResponse.from(require(id)); }
    @Transactional
    public AreaResponse create(AreaRequest request, AsisUserPrincipal actor, RequestMetadata metadata) {
        String code = request.codigo().trim().toUpperCase(java.util.Locale.ROOT);
        if (repository.existsByCodigo(code)) throw new ConflictException("AREA_DUPLICADA", "Código de área registrado");
        Area area = repository.save(Area.crear(code, request.nombre().trim(), request.descripcion(), clock.instant()));
        audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.CREAR_AREA, "AREA",
                area.getId().toString(), null, Map.of("codigo", code), Map.of(), metadata);
        return AreaResponse.from(area);
    }
    @Transactional
    public AreaResponse update(Long id, UpdateAreaRequest request, AsisUserPrincipal actor, RequestMetadata metadata) {
        Area area = require(id);
        String before = area.getNombre();
        area.actualizar(request.nombre().trim(), request.descripcion(), clock.instant());
        audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.MODIFICAR_AREA, "AREA",
                id.toString(), Map.of("nombre", before), Map.of("nombre", area.getNombre()), Map.of(), metadata);
        return AreaResponse.from(area);
    }
    @Transactional
    public void deactivate(Long id, AsisUserPrincipal actor, RequestMetadata metadata) {
        Area area = require(id);
        if (area.getEstado() == EstadoRegistro.INACTIVO) return;
        area.desactivar(clock.instant());
        audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.MODIFICAR_AREA, "AREA",
                id.toString(), Map.of("estado", "ACTIVO"), Map.of("estado", "INACTIVO"), Map.of(), metadata);
    }
    private Area require(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("AREA_NO_EXISTE", "Área no encontrada"));
    }
}

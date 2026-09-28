package com.empresa.asiscontrol.cargos.service;

import com.empresa.asiscontrol.auditoria.entity.AuditAction;
import com.empresa.asiscontrol.auditoria.service.AuditService;
import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import com.empresa.asiscontrol.cargos.dto.*;
import com.empresa.asiscontrol.cargos.entity.Cargo;
import com.empresa.asiscontrol.cargos.repository.CargoRepository;
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
public class CargoService {
    private final CargoRepository repository;
    private final AuditService audit;
    private final Clock clock;
    public CargoService(CargoRepository repository, AuditService audit, Clock clock) {
        this.repository = repository; this.audit = audit; this.clock = clock;
    }
    @Transactional(readOnly = true)
    public List<CargoResponse> list() { return repository.findAll().stream().map(CargoResponse::from).toList(); }
    @Transactional(readOnly = true)
    public void requireActive(Long id) {
        if (get(id).estado() != EstadoRegistro.ACTIVO) {
            throw new ConflictException("CARGO_INACTIVO", "El cargo está inactivo");
        }
    }
    @Transactional(readOnly = true)
    public CargoResponse get(Long id) { return CargoResponse.from(require(id)); }
    @Transactional
    public CargoResponse create(CargoRequest request, AsisUserPrincipal actor, RequestMetadata metadata) {
        String code = request.codigo().trim().toUpperCase(java.util.Locale.ROOT);
        if (repository.existsByCodigo(code)) throw new ConflictException("CARGO_DUPLICADO", "Código de cargo registrado");
        Cargo cargo = repository.save(Cargo.crear(code, request.nombre().trim(), request.descripcion(), clock.instant()));
        audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.CREAR_CARGO, "CARGO",
                cargo.getId().toString(), null, Map.of("codigo", code), Map.of(), metadata);
        return CargoResponse.from(cargo);
    }
    @Transactional
    public CargoResponse update(Long id, UpdateCargoRequest request, AsisUserPrincipal actor, RequestMetadata metadata) {
        Cargo cargo = require(id);
        String before = cargo.getNombre();
        cargo.actualizar(request.nombre().trim(), request.descripcion(), clock.instant());
        audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.MODIFICAR_CARGO, "CARGO",
                id.toString(), Map.of("nombre", before), Map.of("nombre", cargo.getNombre()), Map.of(), metadata);
        return CargoResponse.from(cargo);
    }
    @Transactional
    public void deactivate(Long id, AsisUserPrincipal actor, RequestMetadata metadata) {
        Cargo cargo = require(id);
        if (cargo.getEstado() == EstadoRegistro.INACTIVO) return;
        cargo.desactivar(clock.instant());
        audit.recordByUserId(actor.userId(), actor.getUsername(), AuditAction.MODIFICAR_CARGO, "CARGO",
                id.toString(), Map.of("estado", "ACTIVO"), Map.of("estado", "INACTIVO"), Map.of(), metadata);
    }
    private Cargo require(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("CARGO_NO_EXISTE", "Cargo no encontrado"));
    }
}

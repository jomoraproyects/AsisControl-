package com.empresa.asiscontrol.auditoria.service;

import com.empresa.asiscontrol.auditoria.entity.AuditAction;
import com.empresa.asiscontrol.auditoria.entity.AuditEvent;
import com.empresa.asiscontrol.auditoria.entity.AuditOutcome;
import com.empresa.asiscontrol.auditoria.repository.AuditEventRepository;
import com.empresa.asiscontrol.shared.web.RequestMetadata;
import com.empresa.asiscontrol.usuarios.entity.Usuario;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
public class AuditService {

    private final AuditEventRepository repository;
    private final Clock clock;
    private final EntityManager entityManager;

    public AuditService(AuditEventRepository repository, Clock clock, EntityManager entityManager) {
        this.repository = repository;
        this.clock = clock;
        this.entityManager = entityManager;
    }

    @Transactional
    public void recordByUserId(Long userId, String actor, AuditAction action, String entity,
                               String entityId, Map<String, Object> before, Map<String, Object> after,
                               Map<String, Object> details, RequestMetadata metadata) {
        Usuario user = entityManager.getReference(Usuario.class, userId);
        record(user, actor, action, AuditOutcome.EXITOSO, entity, entityId, before, after, details, metadata);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDenied(Long userId, String actor, String endpoint, String method,
                             RequestMetadata metadata) {
        Usuario user = userId == null ? null : entityManager.getReference(Usuario.class, userId);
        repository.save(AuditEvent.crear(user, actor, AuditAction.ACCESO_DENEGADO, AuditOutcome.FALLIDO,
                "ENDPOINT", endpoint, null, null, Map.of("method", method), clock.instant(), metadata));
    }

    @Transactional
    public void record(
            Usuario usuario,
            String actor,
            AuditAction accion,
            AuditOutcome resultado,
            String entidad,
            String entidadId,
            Map<String, Object> antes,
            Map<String, Object> despues,
            Map<String, Object> detalles,
            RequestMetadata metadata) {
        Instant ahora = clock.instant();
        repository.save(AuditEvent.crear(usuario, actor, accion, resultado, entidad, entidadId,
                antes, despues, detalles, ahora, metadata));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIndependent(
            Usuario usuario,
            String actor,
            AuditAction accion,
            AuditOutcome resultado,
            String entidad,
            String entidadId,
            Map<String, Object> detalles,
            RequestMetadata metadata) {
        repository.save(AuditEvent.crear(usuario, actor, accion, resultado, entidad, entidadId,
                null, null, detalles, clock.instant(), metadata));
    }
}

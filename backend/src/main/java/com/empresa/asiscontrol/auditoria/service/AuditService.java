package com.empresa.asiscontrol.auditoria.service;

import com.empresa.asiscontrol.auditoria.entity.AuditAction;
import com.empresa.asiscontrol.auditoria.entity.AuditEvent;
import com.empresa.asiscontrol.auditoria.entity.AuditOutcome;
import com.empresa.asiscontrol.auditoria.repository.AuditEventRepository;
import com.empresa.asiscontrol.shared.web.RequestMetadata;
import com.empresa.asiscontrol.usuarios.entity.Usuario;
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

    public AuditService(AuditEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
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

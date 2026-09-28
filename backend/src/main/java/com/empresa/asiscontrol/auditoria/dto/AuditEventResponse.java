package com.empresa.asiscontrol.auditoria.dto;

import com.empresa.asiscontrol.auditoria.entity.AuditEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        String actor,
        String accion,
        String resultado,
        String entidad,
        String entidadId,
        Map<String, Object> datosAntes,
        Map<String, Object> datosDespues,
        Map<String, Object> detalles,
        Instant fechaHora,
        String ip,
        String correlationId) {

    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(event.getPublicId(), event.getActor(), event.getAccion().name(),
                event.getResultado().name(), event.getEntidad(), event.getEntidadId(), event.getDatosAntes(),
                event.getDatosDespues(), event.getDetalles(), event.getFechaHora(), event.getIp(),
                event.getCorrelationId());
    }
}


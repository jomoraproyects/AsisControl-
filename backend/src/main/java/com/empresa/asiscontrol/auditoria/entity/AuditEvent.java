package com.empresa.asiscontrol.auditoria.entity;

import com.empresa.asiscontrol.shared.web.RequestMetadata;
import com.empresa.asiscontrol.usuarios.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Immutable;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "auditoria")
@Immutable
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "public_id", nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(length = 100)
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private AuditAction accion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditOutcome resultado;

    @Column(length = 80)
    private String entidad;

    @Column(name = "entidad_id", length = 100)
    private String entidadId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_antes", columnDefinition = "json")
    private Map<String, Object> datosAntes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_despues", columnDefinition = "json")
    private Map<String, Object> datosDespues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> detalles;

    @Column(name = "fecha_hora", nullable = false)
    private Instant fechaHora;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    protected AuditEvent() {
    }

    public static AuditEvent crear(
            Usuario usuario,
            String actor,
            AuditAction accion,
            AuditOutcome resultado,
            String entidad,
            String entidadId,
            Map<String, Object> antes,
            Map<String, Object> despues,
            Map<String, Object> detalles,
            Instant ahora,
            RequestMetadata metadata) {
        AuditEvent event = new AuditEvent();
        event.publicId = UUID.randomUUID();
        event.usuario = usuario;
        event.actor = actor;
        event.accion = accion;
        event.resultado = resultado;
        event.entidad = entidad;
        event.entidadId = entidadId;
        event.datosAntes = emptyToNull(antes);
        event.datosDespues = emptyToNull(despues);
        event.detalles = emptyToNull(detalles);
        event.fechaHora = ahora;
        event.ip = metadata.ip();
        event.userAgent = metadata.userAgent();
        event.correlationId = metadata.correlationId();
        return event;
    }

    private static Map<String, Object> emptyToNull(Map<String, Object> value) {
        return value == null || value.isEmpty()
                ? null
                : Collections.unmodifiableMap(new LinkedHashMap<>(value));
    }

    public UUID getPublicId() { return publicId; }
    public Usuario getUsuario() { return usuario; }
    public String getActor() { return actor; }
    public AuditAction getAccion() { return accion; }
    public AuditOutcome getResultado() { return resultado; }
    public String getEntidad() { return entidad; }
    public String getEntidadId() { return entidadId; }
    public Map<String, Object> getDatosAntes() { return datosAntes; }
    public Map<String, Object> getDatosDespues() { return datosDespues; }
    public Map<String, Object> getDetalles() { return detalles; }
    public Instant getFechaHora() { return fechaHora; }
    public String getIp() { return ip; }
    public String getCorrelationId() { return correlationId; }
}

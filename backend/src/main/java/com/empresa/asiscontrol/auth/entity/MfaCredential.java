package com.empresa.asiscontrol.auth.entity;

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
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "credenciales_mfa")
public class MfaCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "public_id", nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "secreto_cifrado", nullable = false, length = 1024)
    private String secretoCifrado;

    @Column(name = "version_clave", nullable = false, length = 30)
    private String versionClave;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MfaCredentialStatus estado;

    @Column(name = "enrolado_en", nullable = false)
    private Instant enroladoEn;

    @Column(name = "confirmado_en")
    private Instant confirmadoEn;

    @Column(name = "revocado_en")
    private Instant revocadoEn;

    protected MfaCredential() {
    }

    public static MfaCredential pending(Usuario usuario, String encryptedSecret, Instant now) {
        MfaCredential credential = new MfaCredential();
        credential.publicId = UUID.randomUUID();
        credential.usuario = usuario;
        credential.tipo = "TOTP";
        credential.secretoCifrado = encryptedSecret;
        credential.versionClave = "v1";
        credential.estado = MfaCredentialStatus.PENDIENTE;
        credential.enroladoEn = now;
        return credential;
    }

    public void confirm(Instant now) {
        this.estado = MfaCredentialStatus.ACTIVA;
        this.confirmadoEn = now;
    }

    public void revoke(Instant now) {
        this.estado = MfaCredentialStatus.REVOCADA;
        this.revocadoEn = now;
    }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public Usuario getUsuario() { return usuario; }
    public String getSecretoCifrado() { return secretoCifrado; }
    public MfaCredentialStatus getEstado() { return estado; }
    public Instant getConfirmadoEn() { return confirmadoEn; }
}


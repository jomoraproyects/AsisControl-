package com.empresa.asiscontrol.usuarios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "public_id", nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID publicId;

    @Column(name = "nombre_usuario", nullable = false, length = 80)
    private String nombreUsuario;

    @Column(name = "nombre_usuario_normalizado", nullable = false, unique = true, length = 80)
    private String nombreUsuarioNormalizado;

    @Column(length = 254)
    private String correo;

    @Column(name = "correo_normalizado", unique = true, length = 254)
    private String correoNormalizado;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "debe_cambiar_password", nullable = false)
    private boolean debeCambiarPassword;

    @Column(name = "auth_version", nullable = false)
    private long authVersion;

    @Column(name = "ultimo_login_en")
    private Instant ultimoLoginEn;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected Usuario() {
    }

    public static Usuario crear(
            String nombreUsuario,
            String nombreUsuarioNormalizado,
            String correo,
            String correoNormalizado,
            String passwordHash,
            Instant ahora) {
        Usuario usuario = new Usuario();
        usuario.publicId = UUID.randomUUID();
        usuario.nombreUsuario = nombreUsuario;
        usuario.nombreUsuarioNormalizado = nombreUsuarioNormalizado;
        usuario.correo = correo;
        usuario.correoNormalizado = correoNormalizado;
        usuario.passwordHash = passwordHash;
        usuario.activo = true;
        usuario.debeCambiarPassword = true;
        usuario.authVersion = 1;
        usuario.creadoEn = ahora;
        usuario.actualizadoEn = ahora;
        return usuario;
    }

    public void actualizarIdentidad(
            String nombreUsuario,
            String nombreUsuarioNormalizado,
            String correo,
            String correoNormalizado,
            Instant ahora) {
        this.nombreUsuario = nombreUsuario;
        this.nombreUsuarioNormalizado = nombreUsuarioNormalizado;
        this.correo = correo;
        this.correoNormalizado = correoNormalizado;
        this.actualizadoEn = ahora;
    }

    public void cambiarPassword(String nuevoHash, boolean debeCambiar, Instant ahora) {
        this.passwordHash = nuevoHash;
        this.debeCambiarPassword = debeCambiar;
        invalidarAutorizacion(ahora);
    }

    public void desactivar(Instant ahora) {
        this.activo = false;
        invalidarAutorizacion(ahora);
    }

    public void activar(Instant ahora) {
        this.activo = true;
        invalidarAutorizacion(ahora);
    }

    public void invalidarAutorizacion(Instant ahora) {
        this.authVersion++;
        this.actualizadoEn = ahora;
    }

    public void registrarLogin(Instant ahora) {
        this.ultimoLoginEn = ahora;
        this.actualizadoEn = ahora;
    }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getNombreUsuarioNormalizado() { return nombreUsuarioNormalizado; }
    public String getCorreo() { return correo; }
    public String getCorreoNormalizado() { return correoNormalizado; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActivo() { return activo; }
    public boolean isDebeCambiarPassword() { return debeCambiarPassword; }
    public long getAuthVersion() { return authVersion; }
    public Instant getUltimoLoginEn() { return ultimoLoginEn; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}


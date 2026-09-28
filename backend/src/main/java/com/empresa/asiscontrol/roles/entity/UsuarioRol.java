package com.empresa.asiscontrol.roles.entity;

import com.empresa.asiscontrol.usuarios.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "usuario_roles")
public class UsuarioRol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(name = "asignado_en", nullable = false)
    private Instant asignadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignado_por")
    private Usuario asignadoPor;

    @Column(name = "revocado_en")
    private Instant revocadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revocado_por")
    private Usuario revocadoPor;

    protected UsuarioRol() {
    }

    public static UsuarioRol asignar(Usuario usuario, Rol rol, Usuario actor, Instant ahora) {
        UsuarioRol asignacion = new UsuarioRol();
        asignacion.usuario = usuario;
        asignacion.rol = rol;
        asignacion.asignadoPor = actor;
        asignacion.asignadoEn = ahora;
        return asignacion;
    }

    public void revocar(Usuario actor, Instant ahora) {
        this.revocadoPor = actor;
        this.revocadoEn = ahora;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Rol getRol() { return rol; }
    public Instant getAsignadoEn() { return asignadoEn; }
    public Instant getRevocadoEn() { return revocadoEn; }
}


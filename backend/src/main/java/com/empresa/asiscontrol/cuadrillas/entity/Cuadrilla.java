package com.empresa.asiscontrol.cuadrillas.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "cuadrillas")
public class Cuadrilla {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 40) private String codigo;
    @Column(nullable = false, length = 120) private String nombre;
    @Column(length = 500) private String descripcion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private EstadoCuadrilla estado;
    @Column(name = "creado_en", nullable = false) private Instant creadoEn;
    @Column(name = "actualizado_en", nullable = false) private Instant actualizadoEn;
    @Version private long version;
    protected Cuadrilla() {}
    public static Cuadrilla crear(String codigo, String nombre, String descripcion, Instant now) {
        Cuadrilla c = new Cuadrilla(); c.codigo = codigo; c.nombre = nombre; c.descripcion = descripcion;
        c.estado = EstadoCuadrilla.ACTIVA; c.creadoEn = now; c.actualizadoEn = now; return c;
    }
    public void actualizar(String nombre, String descripcion, Instant now) {
        this.nombre = nombre; this.descripcion = descripcion; this.actualizadoEn = now;
    }
    public void desactivar(Instant now) { estado = EstadoCuadrilla.INACTIVA; actualizadoEn = now; }
    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public EstadoCuadrilla getEstado() { return estado; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public long getVersion() { return version; }
}

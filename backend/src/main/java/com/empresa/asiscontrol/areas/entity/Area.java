package com.empresa.asiscontrol.areas.entity;

import com.empresa.asiscontrol.shared.domain.EstadoRegistro;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "areas")
public class Area {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 40) private String codigo;
    @Column(nullable = false, length = 120) private String nombre;
    @Column(length = 500) private String descripcion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private EstadoRegistro estado;
    @Column(name = "creado_en", nullable = false) private Instant creadoEn;
    @Column(name = "actualizado_en", nullable = false) private Instant actualizadoEn;
    @Version private long version;

    protected Area() {}
    public static Area crear(String codigo, String nombre, String descripcion, Instant now) {
        Area area = new Area();
        area.codigo = codigo; area.nombre = nombre; area.descripcion = descripcion;
        area.estado = EstadoRegistro.ACTIVO; area.creadoEn = now; area.actualizadoEn = now;
        return area;
    }
    public void actualizar(String nombre, String descripcion, Instant now) {
        this.nombre = nombre; this.descripcion = descripcion; this.actualizadoEn = now;
    }
    public void desactivar(Instant now) { estado = EstadoRegistro.INACTIVO; actualizadoEn = now; }
    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public EstadoRegistro getEstado() { return estado; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public long getVersion() { return version; }
}

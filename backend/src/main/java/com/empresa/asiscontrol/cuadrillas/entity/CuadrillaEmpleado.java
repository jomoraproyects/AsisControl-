package com.empresa.asiscontrol.cuadrillas.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "cuadrilla_empleado")
public class CuadrillaEmpleado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "cuadrilla_id", nullable = false) private Long cuadrillaId;
    @Column(name = "empleado_id", nullable = false) private Long empleadoId;
    @Column(name = "vigente_desde", nullable = false) private LocalDate vigenteDesde;
    @Column(name = "vigente_hasta") private LocalDate vigenteHasta;
    @Column(name = "asignado_por", nullable = false) private Long asignadoPor;
    @Column(name = "creado_en", nullable = false) private Instant creadoEn;
    protected CuadrillaEmpleado() {}
    public static CuadrillaEmpleado crear(Long crewId, Long employeeId, LocalDate desde, Long actorId, Instant now) {
        CuadrillaEmpleado c = new CuadrillaEmpleado(); c.cuadrillaId = crewId;
        c.empleadoId = employeeId; c.vigenteDesde = desde; c.asignadoPor = actorId; c.creadoEn = now; return c;
    }
    public void cerrar(LocalDate date) {
        if (!date.isAfter(vigenteDesde)) throw new IllegalArgumentException("Vigencia debe avanzar de día");
        vigenteHasta = date;
    }
    public Long getId() { return id; }
    public Long getCuadrillaId() { return cuadrillaId; }
    public Long getEmpleadoId() { return empleadoId; }
    public LocalDate getVigenteDesde() { return vigenteDesde; }
}

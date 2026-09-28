package com.empresa.asiscontrol.cuadrillas.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "cuadrilla_supervisor")
public class CuadrillaSupervisor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "cuadrilla_id", nullable = false) private Long cuadrillaId;
    @Column(name = "supervisor_id", nullable = false) private Long supervisorId;
    @Column(name = "vigente_desde", nullable = false) private LocalDate vigenteDesde;
    @Column(name = "vigente_hasta") private LocalDate vigenteHasta;
    @Column(name = "asignado_por", nullable = false) private Long asignadoPor;
    @Column(name = "creado_en", nullable = false) private Instant creadoEn;
    protected CuadrillaSupervisor() {}
    public static CuadrillaSupervisor crear(Long crewId, Long supervisorId, LocalDate desde, Long actorId, Instant now) {
        CuadrillaSupervisor c = new CuadrillaSupervisor(); c.cuadrillaId = crewId;
        c.supervisorId = supervisorId; c.vigenteDesde = desde; c.asignadoPor = actorId; c.creadoEn = now; return c;
    }
    public void cerrar(LocalDate date) {
        if (!date.isAfter(vigenteDesde)) throw new IllegalArgumentException("Vigencia debe avanzar de día");
        vigenteHasta = date;
    }
    public Long getId() { return id; }
    public Long getCuadrillaId() { return cuadrillaId; }
    public Long getSupervisorId() { return supervisorId; }
    public LocalDate getVigenteDesde() { return vigenteDesde; }
}

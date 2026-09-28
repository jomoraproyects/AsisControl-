package com.empresa.asiscontrol.supervisores.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "supervisor_empleado")
public class SupervisorEmpleado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "supervisor_id", nullable = false) private Long supervisorId;
    @Column(name = "empleado_id", nullable = false) private Long empleadoId;
    @Column(name = "vigente_desde", nullable = false) private LocalDate vigenteDesde;
    @Column(name = "vigente_hasta") private LocalDate vigenteHasta;
    @Column(name = "asignado_por", nullable = false) private Long asignadoPor;
    @Column(name = "motivo_cambio", length = 500) private String motivoCambio;
    @Column(name = "creado_en", nullable = false) private Instant creadoEn;
    protected SupervisorEmpleado() {}
    public static SupervisorEmpleado crear(Long supervisorId, Long empleadoId, LocalDate desde,
            Long actorId, String motivo, Instant now) {
        SupervisorEmpleado row = new SupervisorEmpleado();
        row.supervisorId = supervisorId; row.empleadoId = empleadoId; row.vigenteDesde = desde;
        row.asignadoPor = actorId; row.motivoCambio = motivo; row.creadoEn = now;
        return row;
    }
    public void cerrar(LocalDate hasta, String motivo) {
        if (!hasta.isAfter(vigenteDesde)) throw new IllegalArgumentException("Vigencia debe avanzar de día");
        vigenteHasta = hasta; motivoCambio = motivo;
    }
    public Long getId() { return id; }
    public Long getSupervisorId() { return supervisorId; }
    public Long getEmpleadoId() { return empleadoId; }
    public LocalDate getVigenteDesde() { return vigenteDesde; }
    public LocalDate getVigenteHasta() { return vigenteHasta; }
}

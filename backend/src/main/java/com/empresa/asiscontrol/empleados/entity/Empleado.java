package com.empresa.asiscontrol.empleados.entity;

import com.empresa.asiscontrol.shared.domain.EstadoRegistro;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "empleados")
public class Empleado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(name = "tipo_documento", nullable = false, length = 20)
    private TipoDocumento tipoDocumento;
    @Column(name = "numero_documento_normalizado", nullable = false, length = 30)
    private String numeroDocumentoNormalizado;
    @Column(nullable = false, length = 120) private String nombres;
    @Column(nullable = false, length = 120) private String apellidos;
    @Column(name = "codigo_empleado", nullable = false, length = 40) private String codigoEmpleado;
    @Column(name = "area_id", nullable = false) private Long areaId;
    @Column(name = "cargo_id", nullable = false) private Long cargoId;
    @Enumerated(EnumType.STRING) @Column(name = "tipo_empleado", nullable = false, length = 20)
    private TipoEmpleado tipoEmpleado;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private EstadoRegistro estado;
    @Column(name = "fecha_ingreso", nullable = false) private LocalDate fechaIngreso;
    @Column(name = "fecha_inactivacion") private LocalDate fechaInactivacion;
    @Column(name = "motivo_inactivacion", length = 500) private String motivoInactivacion;
    @Column(name = "creado_en", nullable = false) private Instant creadoEn;
    @Column(name = "actualizado_en", nullable = false) private Instant actualizadoEn;
    @Version private long version;
    protected Empleado() {}
    public static Empleado crear(TipoDocumento tipoDocumento, String documento, String nombres, String apellidos,
            String codigo, Long areaId, Long cargoId, TipoEmpleado tipoEmpleado, LocalDate fechaIngreso, Instant now) {
        Empleado e = new Empleado();
        e.tipoDocumento = tipoDocumento; e.numeroDocumentoNormalizado = documento;
        e.nombres = nombres; e.apellidos = apellidos; e.codigoEmpleado = codigo;
        e.areaId = areaId; e.cargoId = cargoId; e.tipoEmpleado = tipoEmpleado;
        e.estado = EstadoRegistro.ACTIVO; e.fechaIngreso = fechaIngreso;
        e.creadoEn = now; e.actualizadoEn = now;
        return e;
    }
    public void actualizar(String nombres, String apellidos, Long areaId, Long cargoId, Instant now) {
        this.nombres = nombres; this.apellidos = apellidos; this.areaId = areaId;
        this.cargoId = cargoId; this.actualizadoEn = now;
    }
    public void desactivar(LocalDate fecha, String motivo, Instant now) {
        estado = EstadoRegistro.INACTIVO; fechaInactivacion = fecha;
        motivoInactivacion = motivo; actualizadoEn = now;
    }
    public Long getId() { return id; }
    public TipoDocumento getTipoDocumento() { return tipoDocumento; }
    public String getNumeroDocumentoNormalizado() { return numeroDocumentoNormalizado; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getNombreCompleto() { return nombres + " " + apellidos; }
    public String getCodigoEmpleado() { return codigoEmpleado; }
    public Long getAreaId() { return areaId; }
    public Long getCargoId() { return cargoId; }
    public TipoEmpleado getTipoEmpleado() { return tipoEmpleado; }
    public EstadoRegistro getEstado() { return estado; }
    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public LocalDate getFechaInactivacion() { return fechaInactivacion; }
    public String getMotivoInactivacion() { return motivoInactivacion; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public long getVersion() { return version; }
}

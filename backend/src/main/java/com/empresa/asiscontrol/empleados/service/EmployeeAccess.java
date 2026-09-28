package com.empresa.asiscontrol.empleados.service;

import com.empresa.asiscontrol.empleados.dto.*;
import com.empresa.asiscontrol.empleados.entity.*;
import com.empresa.asiscontrol.empleados.repository.EmpleadoRepository;
import com.empresa.asiscontrol.shared.domain.EstadoRegistro;
import com.empresa.asiscontrol.shared.exception.ConflictException;
import com.empresa.asiscontrol.shared.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeAccess {
    private final EmpleadoRepository repository;
    public EmployeeAccess(EmpleadoRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true)
    public EmpleadoResponse get(Long id) { return EmpleadoResponse.from(require(id)); }
    @Transactional(readOnly = true)
    public EmpleadoPerfilResponse profile(Long id) { return EmpleadoPerfilResponse.from(require(id)); }
    @Transactional(readOnly = true)
    public List<EmpleadoResumenResponse> summaries(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return repository.findByIdIn(ids).stream().map(EmpleadoResumenResponse::from).toList();
    }
    @Transactional(readOnly = true)
    public List<EmpleadoResumenResponse> activeSupervisors() {
        return repository.findByTipoEmpleadoAndEstado(TipoEmpleado.SUPERVISOR, EstadoRegistro.ACTIVO)
                .stream().map(EmpleadoResumenResponse::from).toList();
    }
    @Transactional(readOnly = true)
    public void requireActiveType(Long id, TipoEmpleado type) {
        Empleado e = require(id);
        if (e.getEstado() != EstadoRegistro.ACTIVO || e.getTipoEmpleado() != type) {
            throw new ConflictException("TIPO_EMPLEADO_INCOMPATIBLE", "Empleado inactivo o tipo incompatible");
        }
    }
    @Transactional(readOnly = true)
    public void requireActive(Long id) {
        if (require(id).getEstado() != EstadoRegistro.ACTIVO)
            throw new ConflictException("EMPLEADO_INACTIVO", "Empleado inactivo");
    }
    @Transactional
    public void lockActive(Long id) {
        Empleado e = repository.lockById(id).orElseThrow(() ->
                new NotFoundException("EMPLEADO_NO_EXISTE", "Empleado no encontrado"));
        if (e.getEstado() != EstadoRegistro.ACTIVO)
            throw new ConflictException("EMPLEADO_INACTIVO", "Empleado inactivo");
    }
    private Empleado require(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new NotFoundException("EMPLEADO_NO_EXISTE", "Empleado no encontrado"));
    }
}

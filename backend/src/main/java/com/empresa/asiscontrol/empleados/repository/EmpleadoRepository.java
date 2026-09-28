package com.empresa.asiscontrol.empleados.repository;

import com.empresa.asiscontrol.empleados.entity.*;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    boolean existsByTipoDocumentoAndNumeroDocumentoNormalizado(TipoDocumento tipo, String documento);
    boolean existsByCodigoEmpleado(String codigo);
    List<Empleado> findByIdIn(List<Long> ids);
    List<Empleado> findByTipoEmpleadoAndEstado(TipoEmpleado tipo, com.empresa.asiscontrol.shared.domain.EstadoRegistro estado);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Empleado e where e.id = :id")
    Optional<Empleado> lockById(Long id);
}

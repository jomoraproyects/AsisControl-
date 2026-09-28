package com.empresa.asiscontrol.supervisores.repository;

import com.empresa.asiscontrol.supervisores.entity.SupervisorEmpleado;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SupervisorEmpleadoRepository extends JpaRepository<SupervisorEmpleado, Long> {
    Optional<SupervisorEmpleado> findByEmpleadoIdAndVigenteHastaIsNull(Long empleadoId);
    @Query("select s.empleadoId from SupervisorEmpleado s where s.supervisorId = :supervisorId " +
           "and s.vigenteDesde <= :fecha and (s.vigenteHasta is null or s.vigenteHasta > :fecha)")
    List<Long> assignedEmployeeIds(Long supervisorId, LocalDate fecha);
    @Query("select count(s) from SupervisorEmpleado s where s.empleadoId = :empleadoId " +
           "and s.vigenteDesde <= :fecha and (s.vigenteHasta is null or s.vigenteHasta > :fecha) " +
           "and s.supervisorId = :supervisorId")
    long countActive(Long supervisorId, Long empleadoId, LocalDate fecha);
    @Query("select s from SupervisorEmpleado s where s.empleadoId = :empleadoId " +
           "and (s.vigenteHasta is null or s.vigenteHasta > :desde)")
    List<SupervisorEmpleado> overlappingFrom(Long empleadoId, LocalDate desde);
}

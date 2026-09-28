package com.empresa.asiscontrol.cuadrillas.repository;

import com.empresa.asiscontrol.cuadrillas.entity.CuadrillaEmpleado;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CuadrillaEmpleadoRepository extends JpaRepository<CuadrillaEmpleado, Long> {
    Optional<CuadrillaEmpleado> findByEmpleadoIdAndVigenteHastaIsNull(Long employeeId);
    @Query("select c from CuadrillaEmpleado c where c.cuadrillaId = :crewId " +
           "and c.vigenteDesde <= :date and (c.vigenteHasta is null or c.vigenteHasta > :date)")
    List<CuadrillaEmpleado> activeMembers(Long crewId, LocalDate date);
    @Query("select c from CuadrillaEmpleado c where c.empleadoId = :employeeId " +
           "and (c.vigenteHasta is null or c.vigenteHasta > :desde)")
    List<CuadrillaEmpleado> overlappingFrom(Long employeeId, LocalDate desde);
}

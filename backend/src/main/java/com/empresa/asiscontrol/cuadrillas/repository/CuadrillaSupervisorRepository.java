package com.empresa.asiscontrol.cuadrillas.repository;

import com.empresa.asiscontrol.cuadrillas.entity.CuadrillaSupervisor;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CuadrillaSupervisorRepository extends JpaRepository<CuadrillaSupervisor, Long> {
    Optional<CuadrillaSupervisor> findByCuadrillaIdAndVigenteHastaIsNull(Long crewId);
    @Query("select c from CuadrillaSupervisor c where c.cuadrillaId = :crewId " +
           "and c.vigenteDesde <= :date and (c.vigenteHasta is null or c.vigenteHasta > :date)")
    Optional<CuadrillaSupervisor> activeOn(Long crewId, LocalDate date);
    @Query("select c.cuadrillaId from CuadrillaSupervisor c where c.supervisorId = :supervisorId " +
           "and c.vigenteDesde <= :date and (c.vigenteHasta is null or c.vigenteHasta > :date)")
    List<Long> crewsLedOn(Long supervisorId, LocalDate date);
    @Query("select c from CuadrillaSupervisor c where c.cuadrillaId = :crewId " +
           "and (c.vigenteHasta is null or c.vigenteHasta > :desde)")
    List<CuadrillaSupervisor> overlappingFrom(Long crewId, LocalDate desde);
}

package com.empresa.asiscontrol.cuadrillas.repository;

import com.empresa.asiscontrol.cuadrillas.entity.Cuadrilla;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CuadrillaRepository extends JpaRepository<Cuadrilla, Long> {
    boolean existsByCodigo(String codigo);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cuadrilla c where c.id = :id")
    Optional<Cuadrilla> lockById(Long id);
}

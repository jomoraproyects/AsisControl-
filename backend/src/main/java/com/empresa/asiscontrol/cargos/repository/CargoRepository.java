package com.empresa.asiscontrol.cargos.repository;

import com.empresa.asiscontrol.cargos.entity.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CargoRepository extends JpaRepository<Cargo, Long> {
    boolean existsByCodigo(String codigo);
}

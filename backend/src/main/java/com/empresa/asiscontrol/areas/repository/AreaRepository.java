package com.empresa.asiscontrol.areas.repository;

import com.empresa.asiscontrol.areas.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaRepository extends JpaRepository<Area, Long> {
    boolean existsByCodigo(String codigo);
}

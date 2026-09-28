package com.empresa.asiscontrol.roles.repository;

import com.empresa.asiscontrol.roles.entity.Permiso;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {
    List<Permiso> findAllByOrderByCodigoAsc();
}


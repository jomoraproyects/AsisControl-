package com.empresa.asiscontrol.roles.repository;

import com.empresa.asiscontrol.roles.entity.UsuarioRol;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Long> {

    @EntityGraph(attributePaths = {"rol", "rol.permisos"})
    List<UsuarioRol> findByUsuarioIdAndRevocadoEnIsNull(Long usuarioId);

    @EntityGraph(attributePaths = "rol")
    Optional<UsuarioRol> findByUsuarioIdAndRolCodigoAndRevocadoEnIsNull(Long usuarioId, String codigo);
}


package com.empresa.asiscontrol.usuarios.repository;

import com.empresa.asiscontrol.usuarios.entity.Usuario;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByNombreUsuarioNormalizado(String nombreUsuarioNormalizado);

    Optional<Usuario> findByPublicId(UUID publicId);

    boolean existsByNombreUsuarioNormalizado(String nombreUsuarioNormalizado);

    boolean existsByCorreoNormalizado(String correoNormalizado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select distinct u from Usuario u
            join UsuarioRol ur on ur.usuario = u
            join ur.rol r
            where u.activo = true and ur.revocadoEn is null and r.codigo = :codigo
            """)
    List<Usuario> bloquearUsuariosActivosConRol(@Param("codigo") String codigo);
}


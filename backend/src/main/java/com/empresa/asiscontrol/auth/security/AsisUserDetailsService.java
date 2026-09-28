package com.empresa.asiscontrol.auth.security;

import com.empresa.asiscontrol.roles.entity.UsuarioRol;
import com.empresa.asiscontrol.roles.repository.UsuarioRolRepository;
import com.empresa.asiscontrol.shared.util.TextNormalizer;
import com.empresa.asiscontrol.usuarios.entity.Usuario;
import com.empresa.asiscontrol.usuarios.repository.UsuarioRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsisUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;

    public AsisUserDetailsService(
            UsuarioRepository usuarioRepository,
            UsuarioRolRepository usuarioRolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNombreUsuarioNormalizado(TextNormalizer.identifier(username))
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
        List<UsuarioRol> asignaciones = usuarioRolRepository.findByUsuarioIdAndRevocadoEnIsNull(usuario.getId());
        Set<String> roles = new LinkedHashSet<>();
        Set<String> permisos = new LinkedHashSet<>();
        asignaciones.forEach(asignacion -> {
            roles.add(asignacion.getRol().getCodigo());
            asignacion.getRol().getPermisos().forEach(permiso -> permisos.add(permiso.getCodigo()));
        });
        return new AsisUserPrincipal(usuario.getId(), usuario.getPublicId(), usuario.getNombreUsuario(),
                usuario.getPasswordHash(), usuario.isActivo(), usuario.isDebeCambiarPassword(),
                usuario.getAuthVersion(), roles, permisos);
    }
}


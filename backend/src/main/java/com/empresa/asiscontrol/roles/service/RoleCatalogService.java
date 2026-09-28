package com.empresa.asiscontrol.roles.service;

import com.empresa.asiscontrol.roles.dto.RoleResponse;
import com.empresa.asiscontrol.roles.repository.RolRepository;
import com.empresa.asiscontrol.roles.repository.UsuarioRolRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleCatalogService {

    private final RolRepository repository;
    private final UsuarioRolRepository userRoles;

    public RoleCatalogService(RolRepository repository, UsuarioRolRepository userRoles) {
        this.repository = repository;
        this.userRoles = userRoles;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        return repository.findAllByOrderByIdAsc().stream()
                .map(role -> RoleResponse.from(role, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasActiveRole(Long userId, String roleCode) {
        return userRoles.findByUsuarioIdAndRolCodigoAndRevocadoEnIsNull(userId, roleCode).isPresent();
    }
}

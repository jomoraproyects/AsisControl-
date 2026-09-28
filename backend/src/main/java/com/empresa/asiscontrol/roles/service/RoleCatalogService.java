package com.empresa.asiscontrol.roles.service;

import com.empresa.asiscontrol.roles.Roles;
import com.empresa.asiscontrol.roles.dto.RoleResponse;
import com.empresa.asiscontrol.roles.repository.RolRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleCatalogService {

    private final RolRepository repository;

    public RoleCatalogService(RolRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        return repository.findAllByOrderByIdAsc().stream()
                .map(role -> RoleResponse.from(role, Roles.ASIGNABLES_FASE_1.contains(role.getCodigo())))
                .toList();
    }
}


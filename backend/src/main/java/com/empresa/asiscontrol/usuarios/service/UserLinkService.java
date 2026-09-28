package com.empresa.asiscontrol.usuarios.service;

import com.empresa.asiscontrol.empleados.entity.TipoEmpleado;
import com.empresa.asiscontrol.empleados.service.EmployeeAccess;
import com.empresa.asiscontrol.auditoria.entity.AuditAction;
import com.empresa.asiscontrol.auditoria.entity.AuditOutcome;
import com.empresa.asiscontrol.auditoria.service.AuditService;
import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import com.empresa.asiscontrol.auth.service.SessionRevocationService;
import com.empresa.asiscontrol.roles.Roles;
import com.empresa.asiscontrol.roles.service.RoleCatalogService;
import com.empresa.asiscontrol.shared.exception.ConflictException;
import com.empresa.asiscontrol.shared.exception.ForbiddenOperationException;
import com.empresa.asiscontrol.shared.web.RequestMetadata;
import com.empresa.asiscontrol.usuarios.repository.UsuarioRepository;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserLinkService {
    private final UsuarioRepository users;
    private final EmployeeAccess employees;
    private final RoleCatalogService roles;
    private final SessionRevocationService sessions;
    private final AuditService audit;
    private final Clock clock;
    public UserLinkService(UsuarioRepository users, EmployeeAccess employees,
            RoleCatalogService roles, SessionRevocationService sessions,
            AuditService audit, Clock clock) {
        this.users = users; this.employees = employees; this.roles = roles;
        this.sessions = sessions; this.audit = audit; this.clock = clock;
    }
    @Transactional(readOnly = true)
    public Long requireEmployeeId(Long userId) {
        Long id = users.findById(userId).orElseThrow().getEmpleadoId();
        if (id == null) throw new ForbiddenOperationException("EMPLEADO_NO_VINCULADO", "Usuario sin empleado vinculado");
        return id;
    }
    @Transactional(readOnly = true)
    public void validateRolesForEmployee(Long employeeId, Set<String> roles) {
        if (employeeId != null) employees.requireActive(employeeId);
        if (roles.contains(Roles.SUPERVISOR)) {
            if (employeeId == null) throw new ConflictException("EMPLOYEE_LINK_REQUIRED", "El supervisor requiere empleado");
            employees.requireActiveType(employeeId, TipoEmpleado.SUPERVISOR);
        }
        if (roles.contains(Roles.CONDUCTOR)) {
            if (employeeId == null) throw new ConflictException("EMPLOYEE_LINK_REQUIRED", "El conductor requiere empleado");
            employees.requireActiveType(employeeId, TipoEmpleado.CONDUCTOR);
        }
    }
    @Transactional
    public void deactivateLinked(Long employeeId, AsisUserPrincipal actor, RequestMetadata metadata) {
        users.findByEmpleadoId(employeeId).filter(user -> user.isActivo()).ifPresent(user -> {
            boolean isAdmin = roles.hasActiveRole(user.getId(), Roles.SUPER_ADMIN);
            if (isAdmin && users.bloquearUsuariosActivosConRol(Roles.SUPER_ADMIN).size() <= 1)
                throw new ForbiddenOperationException("LAST_SUPER_ADMIN", "No se puede desactivar el último SUPER_ADMIN");
            user.desactivar(clock.instant());
            sessions.revokeAll(user.getNombreUsuario());
            audit.record(users.findById(actor.userId()).orElseThrow(), actor.getUsername(),
                    AuditAction.DESACTIVAR_USUARIO, AuditOutcome.EXITOSO, "USUARIO",
                    user.getPublicId().toString(), Map.of("active", true), Map.of("active", false),
                    Map.of("reason", "EMPLOYEE_DEACTIVATION"), metadata);
        });
    }
}

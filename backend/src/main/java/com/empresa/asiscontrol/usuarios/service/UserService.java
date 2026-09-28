package com.empresa.asiscontrol.usuarios.service;

import com.empresa.asiscontrol.auditoria.entity.AuditAction;
import com.empresa.asiscontrol.auditoria.entity.AuditOutcome;
import com.empresa.asiscontrol.auditoria.service.AuditService;
import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import com.empresa.asiscontrol.auth.service.MfaService;
import com.empresa.asiscontrol.auth.service.SessionRevocationService;
import com.empresa.asiscontrol.roles.Roles;
import com.empresa.asiscontrol.roles.entity.Rol;
import com.empresa.asiscontrol.roles.entity.UsuarioRol;
import com.empresa.asiscontrol.roles.repository.RolRepository;
import com.empresa.asiscontrol.roles.repository.UsuarioRolRepository;
import com.empresa.asiscontrol.shared.exception.AuthenticationFailedException;
import com.empresa.asiscontrol.shared.exception.ConflictException;
import com.empresa.asiscontrol.shared.exception.ForbiddenOperationException;
import com.empresa.asiscontrol.shared.exception.NotFoundException;
import com.empresa.asiscontrol.shared.util.TextNormalizer;
import com.empresa.asiscontrol.shared.web.RequestMetadata;
import com.empresa.asiscontrol.usuarios.dto.ChangePasswordRequest;
import com.empresa.asiscontrol.usuarios.dto.CreateUserRequest;
import com.empresa.asiscontrol.usuarios.dto.UpdateUserRequest;
import com.empresa.asiscontrol.usuarios.dto.UserResponse;
import com.empresa.asiscontrol.usuarios.entity.Usuario;
import com.empresa.asiscontrol.usuarios.repository.UsuarioRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicy;
    private final SessionRevocationService sessions;
    private final MfaService mfaService;
    private final AuditService auditService;
    private final Clock clock;
    private final UserLinkService links;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                       UsuarioRolRepository usuarioRolRepository, PasswordEncoder passwordEncoder,
                       PasswordPolicyService passwordPolicy, SessionRevocationService sessions,
                       MfaService mfaService, AuditService auditService, Clock clock,
                       UserLinkService links) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.sessions = sessions;
        this.mfaService = mfaService;
        this.auditService = auditService;
        this.clock = clock;
        this.links = links;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return usuarioRepository.findAll().stream().map(this::response).toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest input, AsisUserPrincipal actor, RequestMetadata metadata) {
        Usuario actorEntity = requireUser(actor.publicId());
        validateAssignableRoles(input.roles(), input.empleadoId());
        return createInternal(input.username(), input.email(), input.password(), input.roles(), actorEntity,
                actor.getUsername(), metadata, input.empleadoId());
    }

    @Transactional
    public Usuario bootstrap(String username, String email, String password, RequestMetadata metadata) {
        if (usuarioRepository.count() != 0) {
            throw new ConflictException("BOOTSTRAP_NOT_EMPTY",
                    "El bootstrap sólo puede ejecutarse cuando no existe ningún usuario");
        }
        UserResponse response = createInternal(username, email, password, Set.of(Roles.SUPER_ADMIN),
                null, "SERVER_BOOTSTRAP", metadata, null);
        return requireUser(response.id());
    }

    @Transactional
    public UserResponse update(UUID publicId, UpdateUserRequest input, AsisUserPrincipal actor,
                               RequestMetadata metadata) {
        Usuario target = requireUser(publicId);
        Usuario actorEntity = requireUser(actor.publicId());
        String normalizedUsername = TextNormalizer.identifier(input.username());
        String normalizedEmail = TextNormalizer.optionalIdentifier(input.email());
        usuarioRepository.findByNombreUsuarioNormalizado(normalizedUsername)
                .filter(existing -> !existing.getId().equals(target.getId()))
                .ifPresent(existing -> { throw new ConflictException("USERNAME_ALREADY_EXISTS",
                        "El nombre de usuario ya está registrado"); });
        if (normalizedEmail != null && usuarioRepository.existsByCorreoNormalizado(normalizedEmail)
                && !normalizedEmail.equals(target.getCorreoNormalizado())) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "El correo ya está registrado");
        }
        Map<String, Object> before = identitySnapshot(target);
        String previousUsername = target.getNombreUsuario();
        boolean usernameChanged = !normalizedUsername.equals(target.getNombreUsuarioNormalizado());
        target.actualizarIdentidad(input.username().trim(), normalizedUsername, blankToNull(input.email()),
                normalizedEmail, clock.instant());
        if (usernameChanged) {
            target.invalidarAutorizacion(clock.instant());
            sessions.revokeAll(previousUsername);
        }
        auditService.record(actorEntity, actor.getUsername(), AuditAction.MODIFICAR_USUARIO,
                AuditOutcome.EXITOSO, "USUARIO", publicId.toString(), before, identitySnapshot(target),
                Map.of(), metadata);
        return response(target);
    }

    @Transactional
    public void deactivate(UUID publicId, AsisUserPrincipal actor, RequestMetadata metadata) {
        Usuario target = requireUser(publicId);
        Usuario actorEntity = requireUser(actor.publicId());
        if (!target.isActivo()) {
            return;
        }
        protectLastSuperAdmin(target);
        Map<String, Object> before = Map.of("active", true);
        target.desactivar(clock.instant());
        sessions.revokeAll(target.getNombreUsuario());
        auditService.record(actorEntity, actor.getUsername(), AuditAction.DESACTIVAR_USUARIO,
                AuditOutcome.EXITOSO, "USUARIO", publicId.toString(), before, Map.of("active", false),
                Map.of(), metadata);
    }

    @Transactional
    public UserResponse assignRole(UUID publicId, String roleCode, AsisUserPrincipal actor,
                                   RequestMetadata metadata) {
        String normalizedRole = roleCode.trim().toUpperCase();
        Usuario target = requireUser(publicId);
        validateAssignableRoles(Set.of(normalizedRole), target.getEmpleadoId());
        Usuario actorEntity = requireUser(actor.publicId());
        if (usuarioRolRepository.findByUsuarioIdAndRolCodigoAndRevocadoEnIsNull(target.getId(), normalizedRole)
                .isPresent()) {
            throw new ConflictException("ROLE_ALREADY_ASSIGNED", "El usuario ya tiene ese rol");
        }
        Rol role = rolRepository.findByCodigo(normalizedRole)
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Rol no encontrado"));
        usuarioRolRepository.save(UsuarioRol.asignar(target, role, actorEntity, clock.instant()));
        target.invalidarAutorizacion(clock.instant());
        sessions.revokeAll(target.getNombreUsuario());
        auditService.record(actorEntity, actor.getUsername(), AuditAction.ASIGNAR_ROL,
                AuditOutcome.EXITOSO, "USUARIO", publicId.toString(), null, null,
                Map.of("role", normalizedRole), metadata);
        return response(target);
    }

    @Transactional
    public UserResponse revokeRole(UUID publicId, String roleCode, AsisUserPrincipal actor,
                                   RequestMetadata metadata) {
        String normalizedRole = roleCode.trim().toUpperCase();
        Usuario target = requireUser(publicId);
        Usuario actorEntity = requireUser(actor.publicId());
        UsuarioRol assignment = usuarioRolRepository
                .findByUsuarioIdAndRolCodigoAndRevocadoEnIsNull(target.getId(), normalizedRole)
                .orElseThrow(() -> new NotFoundException("ROLE_ASSIGNMENT_NOT_FOUND", "El rol no está asignado"));
        if (Roles.SUPER_ADMIN.equals(normalizedRole) && target.isActivo()) {
            protectLastSuperAdmin(target);
        }
        assignment.revocar(actorEntity, clock.instant());
        target.invalidarAutorizacion(clock.instant());
        sessions.revokeAll(target.getNombreUsuario());
        auditService.record(actorEntity, actor.getUsername(), AuditAction.REVOCAR_ROL,
                AuditOutcome.EXITOSO, "USUARIO", publicId.toString(), null, null,
                Map.of("role", normalizedRole), metadata);
        return response(target);
    }

    @Transactional
    public String resetPassword(UUID publicId, AsisUserPrincipal actor, RequestMetadata metadata) {
        Usuario target = requireUser(publicId);
        Usuario actorEntity = requireUser(actor.publicId());
        String temporary = generateTemporaryPassword();
        target.cambiarPassword(passwordEncoder.encode(temporary), true, clock.instant());
        sessions.revokeAll(target.getNombreUsuario());
        auditService.record(actorEntity, actor.getUsername(), AuditAction.MODIFICAR_USUARIO,
                AuditOutcome.EXITOSO, "USUARIO", publicId.toString(), null, null,
                Map.of("operation", "PASSWORD_RESET"), metadata);
        return temporary;
    }

    @Transactional
    public void changeOwnPassword(AsisUserPrincipal principal, ChangePasswordRequest input,
                                  RequestMetadata metadata) {
        Usuario user = requireUser(principal.publicId());
        if (!passwordEncoder.matches(input.currentPassword(), user.getPasswordHash())) {
            throw new AuthenticationFailedException("La contraseña actual no es válida");
        }
        passwordPolicy.validate(input.newPassword(), user.getNombreUsuario());
        if (passwordEncoder.matches(input.newPassword(), user.getPasswordHash())) {
            throw new ConflictException("PASSWORD_REUSED", "La nueva contraseña debe ser diferente");
        }
        user.cambiarPassword(passwordEncoder.encode(input.newPassword()), false, clock.instant());
        sessions.revokeAll(user.getNombreUsuario());
        auditService.record(user, principal.getUsername(), AuditAction.MODIFICAR_USUARIO,
                AuditOutcome.EXITOSO, "USUARIO", user.getPublicId().toString(), null, null,
                Map.of("operation", "PASSWORD_CHANGE"), metadata);
    }

    @Transactional
    public UserResponse linkEmployee(UUID publicId, Long employeeId, AsisUserPrincipal actor,
                                     RequestMetadata metadata) {
        Usuario target = requireUser(publicId);
        if (usuarioRepository.existsByEmpleadoId(employeeId) && !employeeId.equals(target.getEmpleadoId()))
            throw new ConflictException("EMPLOYEE_ALREADY_LINKED", "El empleado ya está vinculado a un usuario");
        Set<String> roles = usuarioRolRepository.findByUsuarioIdAndRevocadoEnIsNull(target.getId()).stream()
                .map(row -> row.getRol().getCodigo()).collect(java.util.stream.Collectors.toSet());
        links.validateRolesForEmployee(employeeId, roles);
        Long before = target.getEmpleadoId();
        target.vincularEmpleado(employeeId, clock.instant());
        sessions.revokeAll(target.getNombreUsuario());
        auditService.record(requireUser(actor.publicId()), actor.getUsername(), AuditAction.MODIFICAR_USUARIO,
                AuditOutcome.EXITOSO, "USUARIO", publicId.toString(),
                Map.of("empleadoId", before == null ? "SIN_VINCULO" : before),
                Map.of("empleadoId", employeeId), Map.of("operation", "LINK_EMPLOYEE"), metadata);
        return response(target);
    }

    @Transactional
    public UserResponse unlinkEmployee(UUID publicId, AsisUserPrincipal actor, RequestMetadata metadata) {
        Usuario target = requireUser(publicId);
        Set<String> roles = usuarioRolRepository.findByUsuarioIdAndRevocadoEnIsNull(target.getId()).stream()
                .map(row -> row.getRol().getCodigo()).collect(java.util.stream.Collectors.toSet());
        if (roles.contains(Roles.SUPERVISOR) || roles.contains(Roles.CONDUCTOR))
            throw new ConflictException("EMPLOYEE_LINK_REQUIRED", "Revocar primero el rol operativo");
        Long before = target.getEmpleadoId();
        if (before == null) return response(target);
        target.vincularEmpleado(null, clock.instant());
        sessions.revokeAll(target.getNombreUsuario());
        auditService.record(requireUser(actor.publicId()), actor.getUsername(), AuditAction.MODIFICAR_USUARIO,
                AuditOutcome.EXITOSO, "USUARIO", publicId.toString(), Map.of("empleadoId", before),
                Map.of("empleadoId", "SIN_VINCULO"), Map.of("operation", "UNLINK_EMPLOYEE"), metadata);
        return response(target);
    }

    @Transactional
    public void recoverSuperAdmin(String username, String password, boolean resetMfa,
                                  RequestMetadata metadata) {
        Usuario user = usuarioRepository.findByNombreUsuarioNormalizado(TextNormalizer.identifier(username))
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Usuario no encontrado"));
        boolean superAdmin = usuarioRolRepository
                .findByUsuarioIdAndRolCodigoAndRevocadoEnIsNull(user.getId(), Roles.SUPER_ADMIN).isPresent();
        if (!superAdmin) {
            throw new ForbiddenOperationException("RECOVERY_NOT_SUPER_ADMIN",
                    "La recuperación de servidor sólo admite un SUPER_ADMIN existente");
        }
        passwordPolicy.validate(password, user.getNombreUsuario());
        user.activar(clock.instant());
        user.cambiarPassword(passwordEncoder.encode(password), true, clock.instant());
        if (resetMfa) {
            mfaService.revokeForUser(user.getId());
        }
        sessions.revokeAll(user.getNombreUsuario());
        auditService.record(user, "SERVER_RECOVERY", AuditAction.MODIFICAR_USUARIO,
                AuditOutcome.EXITOSO, "USUARIO", user.getPublicId().toString(), null, null,
                Map.of("operation", "SERVER_RECOVERY", "mfaReset", resetMfa), metadata);
    }

    private UserResponse createInternal(String username, String email, String password, Set<String> roleCodes,
                                        Usuario actor, String actorName, RequestMetadata metadata, Long employeeId) {
        String normalizedUsername = TextNormalizer.identifier(username);
        String normalizedEmail = TextNormalizer.optionalIdentifier(email);
        if (usuarioRepository.existsByNombreUsuarioNormalizado(normalizedUsername)) {
            throw new ConflictException("USERNAME_ALREADY_EXISTS", "El nombre de usuario ya está registrado");
        }
        if (normalizedEmail != null && usuarioRepository.existsByCorreoNormalizado(normalizedEmail)) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "El correo ya está registrado");
        }
        passwordPolicy.validate(password, username);
        Instant now = clock.instant();
        Usuario user = Usuario.crear(username.trim(), normalizedUsername,
                blankToNull(email), normalizedEmail, passwordEncoder.encode(password), now);
        if (employeeId != null) user.vincularEmpleado(employeeId, now);
        user = usuarioRepository.save(user);
        List<String> normalizedRoles = roleCodes.stream().map(value -> value.trim().toUpperCase()).sorted().toList();
        for (String code : normalizedRoles) {
            Rol role = rolRepository.findByCodigo(code)
                    .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Rol no encontrado: " + code));
            usuarioRolRepository.save(UsuarioRol.asignar(user, role, actor, now));
        }
        auditService.record(actor, actorName, AuditAction.CREAR_USUARIO, AuditOutcome.EXITOSO,
                "USUARIO", user.getPublicId().toString(), null, identitySnapshot(user), Map.of(), metadata);
        for (String role : normalizedRoles) {
            auditService.record(actor, actorName, AuditAction.ASIGNAR_ROL, AuditOutcome.EXITOSO,
                    "USUARIO", user.getPublicId().toString(), null, null, Map.of("role", role), metadata);
        }
        return response(user);
    }

    private void validateAssignableRoles(Set<String> roleCodes, Long employeeId) {
        Set<String> normalized = new LinkedHashSet<>();
        roleCodes.forEach(value -> normalized.add(value.trim().toUpperCase()));
        if (!Roles.PROTEGIDOS.containsAll(normalized)) {
            throw new NotFoundException("ROLE_NOT_FOUND", "Sólo existen los cuatro roles base protegidos");
        }
        links.validateRolesForEmployee(employeeId, normalized);
    }

    private void protectLastSuperAdmin(Usuario target) {
        boolean hasSuperAdmin = usuarioRolRepository
                .findByUsuarioIdAndRolCodigoAndRevocadoEnIsNull(target.getId(), Roles.SUPER_ADMIN).isPresent();
        if (hasSuperAdmin) {
            List<Usuario> activeAdmins = usuarioRepository.bloquearUsuariosActivosConRol(Roles.SUPER_ADMIN);
            if (activeAdmins.size() <= 1) {
                throw new ForbiddenOperationException("LAST_SUPER_ADMIN",
                        "No se puede desactivar ni revocar el último SUPER_ADMIN activo");
            }
        }
    }

    private UserResponse response(Usuario user) {
        Set<String> roles = usuarioRolRepository.findByUsuarioIdAndRevocadoEnIsNull(user.getId()).stream()
                .map(assignment -> assignment.getRol().getCodigo())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return UserResponse.from(user, roles);
    }

    private Usuario requireUser(UUID publicId) {
        return usuarioRepository.findByPublicId(publicId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Usuario no encontrado"));
    }

    private Map<String, Object> identitySnapshot(Usuario user) {
        Map<String, Object> values = new java.util.LinkedHashMap<>();
        values.put("username", user.getNombreUsuario());
        values.put("email", user.getCorreo());
        values.put("active", user.isActivo());
        return values;
    }

    private String generateTemporaryPassword() {
        byte[] random = new byte[18];
        secureRandom.nextBytes(random);
        return "A!9a" + Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

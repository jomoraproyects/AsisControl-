package com.empresa.asiscontrol.auth.service;

import com.empresa.asiscontrol.auditoria.entity.AuditAction;
import com.empresa.asiscontrol.auditoria.entity.AuditOutcome;
import com.empresa.asiscontrol.auditoria.service.AuditService;
import com.empresa.asiscontrol.auth.dto.AuthenticationResponse;
import com.empresa.asiscontrol.auth.dto.LoginRequest;
import com.empresa.asiscontrol.auth.entity.MfaCredentialStatus;
import com.empresa.asiscontrol.auth.repository.MfaCredentialRepository;
import com.empresa.asiscontrol.auth.security.AsisUserDetailsService;
import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import com.empresa.asiscontrol.shared.config.SecurityProperties;
import com.empresa.asiscontrol.shared.exception.AuthenticationFailedException;
import com.empresa.asiscontrol.shared.util.TextNormalizer;
import com.empresa.asiscontrol.shared.web.RequestMetadata;
import com.empresa.asiscontrol.usuarios.entity.Usuario;
import com.empresa.asiscontrol.usuarios.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    public static final String PENDING_AUTHENTICATION = "ASIS_PENDING_AUTHENTICATION";

    private final AuthenticationManager authenticationManager;
    private final AsisUserDetailsService userDetailsService;
    private final UsuarioRepository usuarioRepository;
    private final MfaCredentialRepository credentialRepository;
    private final AuthenticationRateLimiter rateLimiter;
    private final SecurityContextRepository securityContextRepository;
    private final AuditService auditService;
    private final SecurityProperties properties;
    private final Clock clock;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            AsisUserDetailsService userDetailsService,
            UsuarioRepository usuarioRepository,
            MfaCredentialRepository credentialRepository,
            AuthenticationRateLimiter rateLimiter,
            SecurityContextRepository securityContextRepository,
            AuditService auditService,
            SecurityProperties properties,
            Clock clock) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.usuarioRepository = usuarioRepository;
        this.credentialRepository = credentialRepository;
        this.rateLimiter = rateLimiter;
        this.securityContextRepository = securityContextRepository;
        this.auditService = auditService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public AuthenticationResponse login(LoginRequest input, HttpServletRequest request,
                                        HttpServletResponse response, RequestMetadata metadata) {
        String normalized = TextNormalizer.identifier(input.username());
        String rateKey = "password|" + metadata.ip() + "|" + normalized;
        rateLimiter.check(rateKey);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(input.username(), input.password()));
        } catch (AuthenticationException exception) {
            rateLimiter.failure(rateKey);
            Usuario attempted = usuarioRepository.findByNombreUsuarioNormalizado(normalized).orElse(null);
            auditService.recordIndependent(attempted, input.username(), AuditAction.LOGIN, AuditOutcome.FALLIDO,
                    "USUARIO", attempted == null ? null : attempted.getPublicId().toString(),
                    Map.of("reason", "INVALID_CREDENTIALS"), metadata);
            throw new AuthenticationFailedException("Usuario o contraseña inválidos");
        }
        rateLimiter.success(rateKey);
        AsisUserPrincipal principal = (AsisUserPrincipal) authentication.getPrincipal();
        rotateSession(request);

        if (principal.mustChangePassword()) {
            AsisUserPrincipal limited = new AsisUserPrincipal(principal.userId(), principal.publicId(),
                    principal.getUsername(), null, true, true, principal.authVersion(), Set.of(),
                    Set.of("PASSWORD_CHANGE_REQUIRED"));
            saveAuthentication(limited, request, response);
            return AuthenticationResponse.pending("PASSWORD_CHANGE_REQUIRED", principal);
        }

        if (principal.requiresMfa()) {
            PendingAuthentication pending = new PendingAuthentication(principal.userId(), principal.getUsername(),
                    principal.authVersion(), clock.instant().plus(properties.pendingAuthenticationTtl()));
            request.getSession(true).setAttribute(PENDING_AUTHENTICATION, pending);
            boolean enrolled = credentialRepository
                    .findFirstByUsuarioIdAndEstadoOrderByIdDesc(principal.userId(), MfaCredentialStatus.ACTIVA)
                    .isPresent();
            return AuthenticationResponse.pending(enrolled ? "MFA_REQUIRED" : "MFA_ENROLLMENT_REQUIRED", principal);
        }

        complete(principal, request, response, metadata);
        return AuthenticationResponse.authenticated(principal);
    }

    @Transactional
    public AsisUserPrincipal completePending(PendingAuthentication pending,
                                             HttpServletRequest request,
                                             HttpServletResponse response,
                                             RequestMetadata metadata) {
        AsisUserPrincipal current = (AsisUserPrincipal) userDetailsService.loadUserByUsername(pending.username());
        if (!current.isEnabled() || current.authVersion() != pending.authVersion()) {
            throw new AuthenticationFailedException("El desafío de autenticación ya no es válido");
        }
        complete(current, request, response, metadata);
        return current;
    }

    public PendingAuthentication requirePending(HttpSession session) {
        if (session == null) {
            throw new AuthenticationFailedException("No existe un desafío de autenticación activo");
        }
        Object value = session.getAttribute(PENDING_AUTHENTICATION);
        if (!(value instanceof PendingAuthentication pending)
                || !clock.instant().isBefore(pending.expiresAt())) {
            session.removeAttribute(PENDING_AUTHENTICATION);
            throw new AuthenticationFailedException("El desafío de autenticación expiró");
        }
        Usuario usuario = usuarioRepository.findById(pending.userId())
                .orElseThrow(() -> new AuthenticationFailedException("El desafío ya no es válido"));
        if (!usuario.isActivo() || usuario.getAuthVersion() != pending.authVersion()) {
            session.removeAttribute(PENDING_AUTHENTICATION);
            throw new AuthenticationFailedException("El desafío ya no es válido");
        }
        return pending;
    }

    private void complete(AsisUserPrincipal principal, HttpServletRequest request,
                          HttpServletResponse response, RequestMetadata metadata) {
        rotateSession(request);
        request.getSession(true).removeAttribute(PENDING_AUTHENTICATION);
        saveAuthentication(principal, request, response);
        Usuario usuario = usuarioRepository.findById(principal.userId()).orElseThrow();
        usuario.registrarLogin(clock.instant());
        auditService.record(usuario, principal.getUsername(), AuditAction.LOGIN, AuditOutcome.EXITOSO,
                "USUARIO", principal.publicId().toString(), null, null, Map.of(), metadata);
    }

    @Transactional
    public void logout(AsisUserPrincipal principal, HttpServletRequest request, RequestMetadata metadata) {
        Usuario user = usuarioRepository.findById(principal.userId()).orElse(null);
        auditService.record(user, principal.getUsername(), AuditAction.LOGOUT, AuditOutcome.EXITOSO,
                "USUARIO", principal.publicId().toString(), null, null, Map.of(), metadata);
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
    }

    private void saveAuthentication(AsisUserPrincipal principal, HttpServletRequest request,
                                    HttpServletResponse response) {
        principal.eraseCredentials();
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticated);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private void rotateSession(HttpServletRequest request) {
        request.getSession(true);
        request.changeSessionId();
    }
}

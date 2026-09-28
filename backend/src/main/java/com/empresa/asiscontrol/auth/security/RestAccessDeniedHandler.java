package com.empresa.asiscontrol.auth.security;

import com.empresa.asiscontrol.auditoria.entity.AuditAction;
import com.empresa.asiscontrol.auditoria.entity.AuditOutcome;
import com.empresa.asiscontrol.auditoria.service.AuditService;
import com.empresa.asiscontrol.shared.web.RequestMetadataProvider;
import com.empresa.asiscontrol.usuarios.repository.UsuarioRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityProblemWriter writer;
    private final AuditService auditService;
    private final UsuarioRepository usuarioRepository;
    private final RequestMetadataProvider metadataProvider;

    public RestAccessDeniedHandler(SecurityProblemWriter writer, AuditService auditService,
                                   UsuarioRepository usuarioRepository,
                                   RequestMetadataProvider metadataProvider) {
        this.writer = writer;
        this.auditService = auditService;
        this.usuarioRepository = usuarioRepository;
        this.metadataProvider = metadataProvider;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AsisUserPrincipal principal = authentication != null && authentication.getPrincipal() instanceof AsisUserPrincipal p
                ? p : null;
        auditService.recordIndependent(principal == null ? null : usuarioRepository.findById(principal.userId()).orElse(null),
                principal == null ? null : principal.getUsername(), AuditAction.ACCESO_DENEGADO,
                AuditOutcome.FALLIDO, "ENDPOINT", request.getRequestURI(),
                Map.of("method", request.getMethod()), metadataProvider.from(request));
        writer.write(request, response, HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "No tiene permiso para realizar esta operación");
    }
}

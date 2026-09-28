package com.empresa.asiscontrol.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.empresa.asiscontrol.usuarios.repository.UsuarioRepository;

@Component
public class AuthorizationVersionFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarioRepository;
    private final SecurityProblemWriter writer;

    public AuthorizationVersionFilter(UsuarioRepository usuarioRepository, SecurityProblemWriter writer) {
        this.usuarioRepository = usuarioRepository;
        this.writer = writer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AsisUserPrincipal principal) {
            boolean valid = usuarioRepository.findById(principal.userId())
                    .map(user -> user.isActivo() && user.getAuthVersion() == principal.authVersion())
                    .orElse(false);
            if (!valid) {
                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) {
                    request.getSession(false).invalidate();
                }
                writer.write(request, response, HttpStatus.UNAUTHORIZED, "SESSION_REVOKED",
                        "La sesión fue revocada");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}


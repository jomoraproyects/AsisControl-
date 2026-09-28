package com.empresa.asiscontrol.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.empresa.asiscontrol.auditoria.entity.AuditAction;
import com.empresa.asiscontrol.auditoria.repository.AuditEventRepository;
import com.empresa.asiscontrol.auth.security.AsisUserDetailsService;
import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import com.empresa.asiscontrol.roles.Roles;
import com.empresa.asiscontrol.shared.exception.ForbiddenOperationException;
import com.empresa.asiscontrol.shared.web.RequestMetadata;
import com.empresa.asiscontrol.usuarios.entity.Usuario;
import com.empresa.asiscontrol.usuarios.service.UserService;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

class UserSecurityIntegrationTest extends MySqlIntegrationTest {

    private static final String PASSWORD = "Segura!2026#Clave";
    private static final RequestMetadata METADATA = new RequestMetadata("127.0.0.1", "test", "test-correlation");

    @Autowired UserService userService;
    @Autowired AsisUserDetailsService userDetailsService;
    @Autowired AuditEventRepository auditRepository;

    @Test
    void accesoSinPermisoEsDenegadoYAuditado() throws Exception {
        Usuario conductor = createReadyUser("sinpermiso", PASSWORD, Roles.CONDUCTOR);
        Cookie session = requireCookie(login(conductor));
        long before = auditRepository.countByAccion(AuditAction.ACCESO_DENEGADO);

        mockMvc.perform(get("/api/v1/usuarios").cookie(session))
                .andExpect(status().isForbidden());

        assertThat(auditRepository.countByAccion(AuditAction.ACCESO_DENEGADO)).isEqualTo(before + 1);
    }

    @Test
    void ultimoSuperAdminNoPuedeDesactivarseNiPerderRol() {
        transactions.executeWithoutResult(status -> usuarioRepository
                .bloquearUsuariosActivosConRol(Roles.SUPER_ADMIN)
                .forEach(existing -> existing.desactivar(clock.instant())));
        Usuario admin = createReadyUser("ultimo_admin", PASSWORD, Roles.SUPER_ADMIN);
        AsisUserPrincipal principal = principal(admin);

        assertThatThrownBy(() -> userService.deactivate(admin.getPublicId(), principal, METADATA))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("último SUPER_ADMIN");
        assertThatThrownBy(() -> userService.revokeRole(
                admin.getPublicId(), Roles.SUPER_ADMIN, principal, METADATA))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void cambioDeRolInvalidaSesionExistente() throws Exception {
        Usuario admin = createReadyUser("admin_roles", PASSWORD, Roles.SUPER_ADMIN);
        Usuario target = createReadyUser("target_roles", PASSWORD, Roles.CONDUCTOR);
        Cookie targetSession = requireCookie(login(target));

        userService.assignRole(target.getPublicId(), Roles.RRHH, principal(admin), METADATA);

        mockMvc.perform(get("/api/v1/auth/session").cookie(targetSession))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cambioDePasswordInvalidaSesionYQuedaAuditado() throws Exception {
        Usuario target = createReadyUser("password", PASSWORD, Roles.CONDUCTOR);
        Cookie session = requireCookie(login(target));
        long before = auditRepository.countByAccion(AuditAction.MODIFICAR_USUARIO);

        mockMvc.perform(post("/api/v1/auth/password").cookie(session).with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(
                                new PasswordChange(PASSWORD, "Nueva!2027#Segura"))))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/auth/session").cookie(session))
                .andExpect(status().isUnauthorized());

        assertThat(auditRepository.countByAccion(AuditAction.MODIFICAR_USUARIO)).isEqualTo(before + 1);
    }

    private MvcResult login(Usuario user) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new Credentials(user.getNombreUsuario(), PASSWORD))))
                .andExpect(status().isOk()).andReturn();
    }

    private Cookie requireCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie("asis_test_session");
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private AsisUserPrincipal principal(Usuario user) {
        return (AsisUserPrincipal) userDetailsService.loadUserByUsername(user.getNombreUsuario());
    }

    private record Credentials(String username, String password) {}
    private record PasswordChange(String currentPassword, String newPassword) {}
}

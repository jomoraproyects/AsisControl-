package com.empresa.asiscontrol.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.empresa.asiscontrol.auditoria.entity.AuditAction;
import com.empresa.asiscontrol.auditoria.repository.AuditEventRepository;
import com.empresa.asiscontrol.roles.Roles;
import com.empresa.asiscontrol.usuarios.entity.Usuario;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

class AuthenticationIntegrationTest extends MySqlIntegrationTest {

    private static final String PASSWORD = "Segura!2026#Clave";

    @Autowired AuditEventRepository auditRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void loginCorrectoCreaSesionYLogoutLaInvalida() throws Exception {
        Usuario user = createReadyUser("conductor", PASSWORD, Roles.CONDUCTOR);

        MvcResult login = login(user.getNombreUsuario(), PASSWORD, "AUTHENTICATED");
        Cookie sessionCookie = requireSessionCookie(login);

        mockMvc.perform(get("/api/v1/auth/session").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(user.getNombreUsuario()))
                .andExpect(jsonPath("$.roles[0]").value(Roles.CONDUCTOR));

        mockMvc.perform(post("/api/v1/auth/logout").cookie(sessionCookie).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/auth/session").cookie(sessionCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginIncorrectoEInactivoSonRechazadosYAuditados() throws Exception {
        Usuario active = createReadyUser("activo", PASSWORD, Roles.CONDUCTOR);
        Usuario inactive = createInactiveUser("inactivo", PASSWORD, Roles.CONDUCTOR);
        long before = auditRepository.countByAccion(AuditAction.LOGIN);

        mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(
                                new Credentials(active.getNombreUsuario().toUpperCase(), "Incorrecta!2026"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(
                                new Credentials(inactive.getNombreUsuario(), PASSWORD))))
                .andExpect(status().isUnauthorized());

        org.assertj.core.api.Assertions.assertThat(auditRepository.countByAccion(AuditAction.LOGIN))
                .isEqualTo(before + 2);
    }

    @Test
    void csrfEsObligatorioInclusoParaLogin() throws Exception {
        Usuario user = createReadyUser("csrf", PASSWORD, Roles.CONDUCTOR);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new Credentials(user.getNombreUsuario(), PASSWORD))))
                .andExpect(status().isForbidden());
    }

    @Test
    void sesionExpiradaNoPuedeReutilizarse() throws Exception {
        Usuario user = createReadyUser("expira", PASSWORD, Roles.CONDUCTOR);
        Cookie cookie = requireSessionCookie(login(user.getNombreUsuario(), PASSWORD, "AUTHENTICATED"));
        jdbcTemplate.update("UPDATE SPRING_SESSION SET EXPIRY_TIME = 0 WHERE PRINCIPAL_NAME = ?",
                user.getNombreUsuario());

        mockMvc.perform(get("/api/v1/auth/session").cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    private MvcResult login(String username, String password, String expectedStatus) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new Credentials(username, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andReturn();
    }

    private Cookie requireSessionCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie("asis_test_session");
        org.assertj.core.api.Assertions.assertThat(cookie).isNotNull();
        return cookie;
    }

    private record Credentials(String username, String password) {
    }
}


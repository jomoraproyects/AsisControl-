package com.empresa.asiscontrol.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.empresa.asiscontrol.roles.Roles;
import com.empresa.asiscontrol.usuarios.entity.Usuario;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class MfaIntegrationTest extends MySqlIntegrationTest {

    private static final String PASSWORD = "Segura!2026#Clave";

    @Test
    void enrolaTotpAutenticaYConsumeCodigoDeRecuperacionUnaSolaVez() throws Exception {
        Usuario user = createReadyUser("rrhh", PASSWORD, Roles.RRHH);
        MvcResult initialLogin = passwordLogin(user.getNombreUsuario(), "MFA_ENROLLMENT_REQUIRED");
        Cookie pendingCookie = requireCookie(initialLogin);

        MvcResult enrollment = mockMvc.perform(post("/api/v1/auth/mfa/enroll")
                        .cookie(pendingCookie).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secretBase32").isNotEmpty())
                .andReturn();
        String secretBase32 = objectMapper.readTree(enrollment.getResponse().getContentAsByteArray())
                .get("secretBase32").asText();
        String code = currentCode(secretBase32);

        MvcResult confirmation = mockMvc.perform(post("/api/v1/auth/mfa/confirm")
                        .cookie(pendingCookie).with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new Code(code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authentication.status").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.recoveryCodes.length()").value(8))
                .andReturn();
        String recoveryCode = objectMapper.readTree(confirmation.getResponse().getContentAsByteArray())
                .get("recoveryCodes").get(0).asText();
        Cookie authenticatedCookie = cookieOrFallback(confirmation, pendingCookie);

        mockMvc.perform(post("/api/v1/auth/logout").cookie(authenticatedCookie).with(csrf()))
                .andExpect(status().isNoContent());

        Cookie recoveryPending = requireCookie(passwordLogin(user.getNombreUsuario(), "MFA_REQUIRED"));
        MvcResult recoveryLogin = mockMvc.perform(post("/api/v1/auth/mfa/verify")
                        .cookie(recoveryPending).with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new Verification(null, recoveryCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andReturn();
        Cookie recoveryAuthenticated = cookieOrFallback(recoveryLogin, recoveryPending);
        mockMvc.perform(post("/api/v1/auth/logout").cookie(recoveryAuthenticated).with(csrf()))
                .andExpect(status().isNoContent());

        Cookie reusedPending = requireCookie(passwordLogin(user.getNombreUsuario(), "MFA_REQUIRED"));
        mockMvc.perform(post("/api/v1/auth/mfa/verify")
                        .cookie(reusedPending).with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new Verification(null, recoveryCode))))
                .andExpect(status().isUnauthorized());

        Cookie totpPending = requireCookie(passwordLogin(user.getNombreUsuario(), "MFA_REQUIRED"));
        mockMvc.perform(post("/api/v1/auth/mfa/verify")
                        .cookie(totpPending).with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new Verification(currentCode(secretBase32), null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"));
    }

    private MvcResult passwordLogin(String username, String expectedStatus) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new Credentials(username, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andReturn();
    }

    private String currentCode(String encodedSecret) throws Exception {
        TimeBasedOneTimePasswordGenerator generator = new TimeBasedOneTimePasswordGenerator();
        SecretKey key = new SecretKeySpec(new Base32().decode(encodedSecret), generator.getAlgorithm());
        return generator.generateOneTimePasswordString(key, Instant.now());
    }

    private Cookie requireCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie("asis_test_session");
        org.assertj.core.api.Assertions.assertThat(cookie).isNotNull();
        return cookie;
    }

    private Cookie cookieOrFallback(MvcResult result, Cookie fallback) {
        Cookie changed = result.getResponse().getCookie("asis_test_session");
        return changed == null ? fallback : changed;
    }

    private record Credentials(String username, String password) {}
    private record Code(String code) {}
    private record Verification(String totpCode, String recoveryCode) {}
}


package com.empresa.asiscontrol.usuarios.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.empresa.asiscontrol.shared.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

class PasswordPolicyServiceTest {

    private final PasswordPolicyService policy = new PasswordPolicyService();

    @Test
    void acceptsStrongPassword() {
        assertThatCode(() -> policy.validate("Correcta!2026#", "operador")).doesNotThrowAnyException();
    }

    @Test
    void rejectsWeakPasswordAndUsernameReuse() {
        assertThatThrownBy(() -> policy.validate("short", "operador"))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> policy.validate("Operador!2026#", "operador"))
                .isInstanceOf(InvalidRequestException.class);
    }
}


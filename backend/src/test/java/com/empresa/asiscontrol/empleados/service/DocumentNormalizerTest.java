package com.empresa.asiscontrol.empleados.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.empresa.asiscontrol.shared.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

class DocumentNormalizerTest {
    @Test
    void normalizaSeparadoresYMayusculas() {
        assertThat(DocumentNormalizer.normalize(" ab-123.45 ")).isEqualTo("AB12345");
    }
    @Test
    void rechazaCaracteresNoPermitidos() {
        assertThatThrownBy(() -> DocumentNormalizer.normalize("12/34"))
                .isInstanceOf(InvalidRequestException.class);
    }
}

package com.empresa.asiscontrol.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextNormalizerTest {

    @Test
    void normalizesCaseWhitespaceAndUnicodeCompatibility() {
        assertThat(TextNormalizer.identifier("  AdMİN  ")).isEqualTo("admi̇n");
        assertThat(TextNormalizer.identifier("ＡＤＭＩＮ")).isEqualTo("admin");
    }

    @Test
    void optionalIdentifierConvertsBlankToNull() {
        assertThat(TextNormalizer.optionalIdentifier("  ")).isNull();
    }
}


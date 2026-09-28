package com.empresa.asiscontrol.shared.util;

import java.text.Normalizer;
import java.util.Locale;

public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String identifier(String value) {
        if (value == null) {
            return null;
        }
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
    }

    public static String optionalIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return identifier(value);
    }
}


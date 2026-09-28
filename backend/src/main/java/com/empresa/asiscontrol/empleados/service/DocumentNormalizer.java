package com.empresa.asiscontrol.empleados.service;

import com.empresa.asiscontrol.shared.exception.InvalidRequestException;
import java.text.Normalizer;
import java.util.Locale;

public final class DocumentNormalizer {
    private DocumentNormalizer() {}
    public static String normalize(String raw) {
        if (raw == null) throw new InvalidRequestException("DOCUMENTO_INVALIDO", "Documento requerido");
        String value = Normalizer.normalize(raw, Normalizer.Form.NFKC)
                .toUpperCase(Locale.ROOT).replaceAll("[\\s.\\-]", "");
        if (!value.matches("[A-Z0-9]{3,30}")) {
            throw new InvalidRequestException("DOCUMENTO_INVALIDO", "Documento inválido");
        }
        return value;
    }
}

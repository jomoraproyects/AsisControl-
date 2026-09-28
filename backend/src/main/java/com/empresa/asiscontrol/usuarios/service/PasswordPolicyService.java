package com.empresa.asiscontrol.usuarios.service;

import com.empresa.asiscontrol.shared.exception.InvalidRequestException;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {

    public void validate(String password, String username) {
        boolean valid = password != null
                && password.length() >= 12
                && password.length() <= 200
                && password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isDigit)
                && password.chars().anyMatch(value -> !Character.isLetterOrDigit(value));
        if (!valid) {
            throw new InvalidRequestException("WEAK_PASSWORD",
                    "La contraseña debe tener al menos 12 caracteres, mayúscula, minúscula, número y símbolo");
        }
        if (username != null && password.toLowerCase(Locale.ROOT)
                .contains(username.toLowerCase(Locale.ROOT))) {
            throw new InvalidRequestException("WEAK_PASSWORD",
                    "La contraseña no puede contener el nombre de usuario");
        }
    }
}


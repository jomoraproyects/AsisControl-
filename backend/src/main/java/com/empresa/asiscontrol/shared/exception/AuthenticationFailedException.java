package com.empresa.asiscontrol.shared.exception;

import org.springframework.http.HttpStatus;

public final class AuthenticationFailedException extends DomainException {

    public AuthenticationFailedException(String message) {
        super("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, message);
    }
}


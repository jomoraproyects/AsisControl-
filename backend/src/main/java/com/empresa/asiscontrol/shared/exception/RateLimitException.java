package com.empresa.asiscontrol.shared.exception;

import org.springframework.http.HttpStatus;

public final class RateLimitException extends DomainException {

    public RateLimitException() {
        super("TOO_MANY_ATTEMPTS", HttpStatus.TOO_MANY_REQUESTS,
                "Demasiados intentos. Intente nuevamente más tarde");
    }
}


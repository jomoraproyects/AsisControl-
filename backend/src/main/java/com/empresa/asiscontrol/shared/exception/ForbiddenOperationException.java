package com.empresa.asiscontrol.shared.exception;

import org.springframework.http.HttpStatus;

public final class ForbiddenOperationException extends DomainException {

    public ForbiddenOperationException(String code, String message) {
        super(code, HttpStatus.FORBIDDEN, message);
    }
}


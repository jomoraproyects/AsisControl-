package com.empresa.asiscontrol.shared.exception;

import org.springframework.http.HttpStatus;

public final class ConflictException extends DomainException {

    public ConflictException(String code, String message) {
        super(code, HttpStatus.CONFLICT, message);
    }
}


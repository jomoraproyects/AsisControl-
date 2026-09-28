package com.empresa.asiscontrol.shared.exception;

import org.springframework.http.HttpStatus;

public final class NotFoundException extends DomainException {

    public NotFoundException(String code, String message) {
        super(code, HttpStatus.NOT_FOUND, message);
    }
}


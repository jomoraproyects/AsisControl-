package com.empresa.asiscontrol.shared.exception;

import org.springframework.http.HttpStatus;

public final class InvalidRequestException extends DomainException {

    public InvalidRequestException(String code, String message) {
        super(code, HttpStatus.BAD_REQUEST, message);
    }
}


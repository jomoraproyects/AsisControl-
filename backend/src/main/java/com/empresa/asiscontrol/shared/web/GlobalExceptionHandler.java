package com.empresa.asiscontrol.shared.web;

import com.empresa.asiscontrol.shared.exception.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomain(DomainException exception, HttpServletRequest request) {
        ProblemDetail detail = base(exception.status(), exception.code(), exception.getMessage(), request);
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail detail = base(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "La solicitud contiene datos inválidos", request);
        List<ValidationIssue> issues = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toIssue)
                .toList();
        detail.setProperty("errors", issues);
        return detail;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail handleMalformed(Exception exception, HttpServletRequest request) {
        return base(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "La solicitud no tiene un formato válido", request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unhandled request failure", exception);
        return base(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocurrió un error interno", request);
    }

    private ValidationIssue toIssue(FieldError error) {
        return new ValidationIssue(error.getField(), error.getDefaultMessage());
    }

    private ProblemDetail base(HttpStatus status, String code, String message, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        detail.setType(URI.create("urn:asiscontrol:error:" + code.toLowerCase()));
        detail.setInstance(URI.create(request.getRequestURI()));
        detail.setProperty("code", code);
        Object correlation = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        detail.setProperty("correlationId", correlation == null ? "unknown" : correlation.toString());
        return detail;
    }

    record ValidationIssue(String field, String message) {
    }
}

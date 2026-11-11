package com.tfi.motorsport.backend.handlers.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio con un mensaje claro para el usuario
 * (ej: "el email ya está registrado", "la zona horaria no es válida").
 * Por defecto responde HTTP 400, pero puede indicarse otro estado (ej: 409 Conflict).
 */
public class RuleBusinessException extends RuntimeException {

    private final HttpStatus httpStatus;

    public RuleBusinessException(String message) {

        this(message, HttpStatus.BAD_REQUEST);
    }

    public RuleBusinessException(String message, HttpStatus httpStatus) {

        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getEstado() {
        return httpStatus;
    }
}

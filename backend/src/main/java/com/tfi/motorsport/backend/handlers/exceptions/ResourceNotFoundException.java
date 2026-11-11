package com.tfi.motorsport.backend.handlers.exceptions;

/**
 * Se lanza cuando se busca una entidad (categoria, evento, piloto, etc.) que no existe.
 * El GlobalExceptionHandler la traduce a HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String recurso, Object id) {
        super("No se encontró " + recurso + " con ID " + id);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

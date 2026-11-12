package com.tfi.motorsport.backend.handlers;


import com.tfi.motorsport.backend.dtos.error.ErrorDto;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
/**
 * Traduce las excepciones de toda la API a respuestas JSON uniformes ({@link ErrorDto}).
 *
 * Importante: el orden de resolución de Spring elige el handler MÁS ESPECÍFICO,
 * por eso el handler genérico de {@code Exception} solo atrapa lo que no se manejó antes.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // Maneja errores de validación (Bean Validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        ErrorDto errorDTO = ErrorDto.of(HttpStatus.BAD_REQUEST.value(),
                "Error de validación", detalles);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDTO);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDto> handleNoEncontrado(ResourceNotFoundException ex) {
        return responder(HttpStatus.NOT_FOUND, "Recurso no encontrado", List.of(ex.getMessage()));
    }

    @ExceptionHandler(RuleBusinessException.class)
    public ResponseEntity<ErrorDto> handleReglaNegocio(RuleBusinessException ex) {
        return responder(ex.getEstado(), "Operación no permitida", List.of(ex.getMessage()));
    }

    // Maneja errores de ConstraintViolation (en otros contextos)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorDto> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> detalles = ex.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();

        ErrorDto errorDTO = ErrorDto.of(HttpStatus.BAD_REQUEST.value(),
                "Error de validación", detalles);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDTO);
    }
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorDto> handleNoResource(NoResourceFoundException ex) {
        return responder(HttpStatus.NOT_FOUND, "Ruta no encontrada", List.of("/" + ex.getResourcePath()));
    }

    // Maneja IllegalArgumentException (por ejemplo en validaciones manuales)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDto> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorDto errorDTO = ErrorDto.simple(HttpStatus.BAD_REQUEST.value(),
                "Argumento inválido", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDTO);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDto> handleAccesoDenegado(AccessDeniedException ex) {
        return responder(HttpStatus.FORBIDDEN, "Acceso denegado",
                List.of("No tiene permisos para realizar esta operación"));
    }

    // Maneja cualquier otro error no controlado
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleGeneric(Exception ex) {
        ErrorDto errorDTO = ErrorDto.simple(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Error interno del servidor", ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDTO);
    }

    private ResponseEntity<ErrorDto> responder(HttpStatus estado, String error, List<String> detalles) {
        return ResponseEntity.status(estado).body(ErrorDto.of(estado.value(), error, detalles));
    }
}

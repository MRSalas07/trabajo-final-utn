package com.tfi.motorsport.backend.handlers;


import com.tfi.motorsport.backend.dtos.error.ErrorDto;
import com.tfi.motorsport.backend.handlers.exceptions.IntegrationExternalException;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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
    public ResponseEntity<ErrorDto> handleResourceNotFound(ResourceNotFoundException ex) {
        return responder(HttpStatus.NOT_FOUND, "Recurso no encontrado", List.of(ex.getMessage()));
    }

    @ExceptionHandler(RuleBusinessException.class)
    public ResponseEntity<ErrorDto> handleRuleBusiness(RuleBusinessException ex) {
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
    public ResponseEntity<ErrorDto> handleAccesDenied(AccessDeniedException ex) {
        return responder(HttpStatus.FORBIDDEN, "Acceso denegado",
                List.of("No tiene permisos para realizar esta operación"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorDto> handleMissingParam(MissingServletRequestParameterException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Parámetro faltante",
                List.of("Falta el parámetro obligatorio '" + ex.getParameterName() + "'"));
    }

    /** JSON mal formado o tipos incorrectos en el cuerpo. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDto> handleNotReadable(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Cuerpo de la petición inválido",
                List.of("El JSON enviado no es válido o tiene tipos incorrectos"));
    }

    /**
     * Restricciones (@Min, @Max, @Size...) sobre parámetros de la URL o de la query.
     * Ej: GET /api/categorias/F1/temporadas/1800/eventos (año fuera de rango).
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorDto> handleMethodValidation(HandlerMethodValidationException ex) {
        List<String> detalles = ex.getParameterValidationResults().stream()
                .flatMap(resultado -> resultado.getResolvableErrors().stream()
                        .map(error -> resultado.getMethodParameter().getParameterName()
                                + ": " + error.getDefaultMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "Error de validación", detalles);
    }

    /** Parámetro con un tipo que no se puede convertir. Ej: GET /api/eventos/abc */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDto> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Parámetro inválido",
                List.of("El parámetro '" + ex.getName() + "' tiene un valor inválido: '" + ex.getValue() + "'"));
    }

    /** Método HTTP no soportado por la ruta. Ej: POST /api/salud */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorDto> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido",
                List.of("La ruta no admite el método " + ex.getMethod()));
    }

    /**
     * Violación de una restricción de la base (índice único, clave foránea, NOT NULL).
     * Normalmente la valida el servicio antes; esto cubre los casos concurrentes.
     * No se devuelve el mensaje de la base porque expone nombres de tablas y columnas.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDto> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "Conflicto con datos existentes",
                List.of("La operación entra en conflicto con datos ya registrados"));
    }

    /**
     * Cualquier otro error no controlado. El detalle queda en el log del servidor
     * y el cliente recibe un mensaje fijo: el mensaje real puede exponer detalles internos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleGeneric(Exception ex) {
        log.error("Error no controlado", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor",
                List.of("Ocurrió un error inesperado. Intente nuevamente más tarde."));
    }


    /** Errores de las APIs externas: se informa el proveedor pero no detalles internos. */
    @ExceptionHandler(IntegrationExternalException.class)
    public ResponseEntity<ErrorDto> handleIntegracion(IntegrationExternalException ex) {
        log.warn("Fallo de integración externa: {}", ex.getMessage());
        return responder(ex.getTipo().getEstadoHttp(), "Error en proveedor externo de datos",
                List.of(ex.getMessage()));
    }

    private ResponseEntity<ErrorDto> responder(HttpStatus estado, String error, List<String> detalles) {
        return ResponseEntity.status(estado).body(ErrorDto.of(estado.value(), error, detalles));
    }
}

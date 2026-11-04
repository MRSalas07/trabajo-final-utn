package com.tfi.motorsport.backend.handlers;


import com.tfi.motorsport.backend.dtos.error.ErrorDto;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

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

    // Maneja IllegalArgumentException (por ejemplo en validaciones manuales)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDto> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorDto errorDTO = ErrorDto.simple(HttpStatus.BAD_REQUEST.value(),
                "Argumento inválido", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDTO);
    }

    // Maneja cualquier otro error no controlado
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleGeneric(Exception ex) {
        ErrorDto errorDTO = ErrorDto.simple(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Error interno del servidor", ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDTO);
    }
}

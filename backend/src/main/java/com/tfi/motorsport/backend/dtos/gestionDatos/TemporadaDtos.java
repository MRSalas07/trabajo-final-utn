package com.tfi.motorsport.backend.dtos.gestionDatos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTOs del ABM de temporadas.
 *  - Request: lo que envía el formulario (validado con Bean Validation).
 *  - Registro: la temporada completa tal como se edita.
 */
public final class TemporadaDtos {

    private TemporadaDtos() {
    }

    public record Request(
            @NotNull(message = "La categoría es obligatoria") Long categoriaId,
            @NotNull @Min(1950) @Max(2100) Integer anio,
            @Size(max = 120) String nombre,
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
    }

    public record Registro(Long id, Long categoriaId, String categoria, Integer anio, String nombre,
                           LocalDate fechaInicio, LocalDate fechaFin) {
    }
}


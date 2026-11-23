package com.tfi.motorsport.backend.dtos.gestionDatos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTOs del ABM de tripulaciones de rally (piloto + copiloto + equipo en una temporada).
 */
public final class TripulacionDtos {

    private TripulacionDtos() {
    }

    public record Request(
            @NotNull Long categoriaId,
            @NotNull @Min(1950) @Max(2100) Integer temporada,
            @NotNull(message = "El piloto es obligatorio") Long pilotoId,
            @NotNull(message = "El copiloto es obligatorio") Long copilotoId,
            @NotNull(message = "El equipo es obligatorio") Long equipoId,
            @Min(0) @Max(999) Integer numero
    ) {
    }

    public record Registro(Long id, Long categoriaId, String categoria, Integer temporada, Long pilotoId,
                           String piloto, Long copilotoId, String copiloto, Long equipoId, String equipo,
                           Integer numero,
                           AuditoriaDto auditoria) {
    }
}

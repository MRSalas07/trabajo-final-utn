package com.tfi.motorsport.backend.dtos.gestionDatos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * DTOs del ABM de participaciones (tabla equipo_x_piloto: un piloto en un equipo en una temporada).
 */
public final class ParticipacionDtos {

    private ParticipacionDtos() {
    }

    public record Request(
            @NotNull Long equipoId,
            @NotNull Long pilotoId,
            @NotNull Long categoriaId,
            @NotNull @Min(1950) @Max(2100) Integer temporada,
            @Min(0) @Max(999) Integer numero,
            @Pattern(regexp = "PILOTO|COPILOTO|RESERVA") String rol,
            LocalDate fechaDesde,
            LocalDate fechaHasta
    ) {
    }

    public record Registro(Long id, Long equipoId, String equipo, Long pilotoId, String piloto,
                           Long categoriaId, String categoria, Integer temporada, Integer numero,
                           String rol, LocalDate fechaDesde, LocalDate fechaHasta) {
    }
}


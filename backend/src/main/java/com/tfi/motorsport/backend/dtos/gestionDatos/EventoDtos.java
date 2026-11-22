package com.tfi.motorsport.backend.dtos.gestionDatos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

import static com.tfi.motorsport.backend.dtos.gestionDatos.ValidacionesAbm.ESTADOS;

/**
 * DTOs del ABM de eventos (un Gran Premio, un rally).
 * Las fechas-hora son Instant en UTC (ej: "2026-03-08T04:00:00Z").
 */
public final class EventoDtos {

    private EventoDtos() {
    }

    public record Request(
            @NotNull(message = "La temporada es obligatoria") Long temporadaId,
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String nombre,
            @Size(max = 60) String nombreCorto,
            @Min(1) @Max(99) Integer numeroFecha,
            @Size(max = 80) String pais,
            @Size(max = 80) String ciudad,
            @Size(max = 150) String circuito,
            /** Zona IANA de la sede, ej: "America/Argentina/Buenos_Aires". */
            @Size(max = 64) String zonaHoraria,
            @NotNull(message = "La fecha de inicio es obligatoria") Instant inicio,
            Instant fin,
            @Pattern(regexp = ESTADOS) String estado
    ) {
    }

    public record Registro(Long id, Long temporadaId, Integer anio, String categoria, String idExterno,
                           String nombre, String nombreCorto, Integer numeroFecha, String pais, String ciudad,
                           String circuito, String zonaHoraria, Instant inicio, Instant fin, String estado) {
    }
}


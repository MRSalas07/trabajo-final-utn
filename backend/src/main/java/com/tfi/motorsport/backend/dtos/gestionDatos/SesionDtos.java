package com.tfi.motorsport.backend.dtos.gestionDatos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

import static com.tfi.motorsport.backend.dtos.gestionDatos.ValidacionesAbm.ESTADOS;

/**
 * DTOs del ABM de sesiones (práctica, clasificación, sprint, carrera...).
 */
public final class SesionDtos {

    private SesionDtos() {
    }

    public record Request(
            @NotNull(message = "El evento es obligatorio") Long eventoId,
            @NotBlank @Size(max = 80) String nombre,
            @NotBlank @Pattern(regexp = "PRACTICA|CLASIFICACION|SPRINT_CLASIFICACION|SPRINT|SERIE|CARRERA",
                    message = "Tipo inválido") String tipo,
            @Min(1) @Max(20) Integer numero,
            Instant inicio,
            Instant fin,
            @Min(0) @Max(500) Integer vueltasProgramadas,
            @DecimalMin("0") BigDecimal distancia,
            @Pattern(regexp = ESTADOS) String estado
    ) {
    }

    public record Registro(Long id, Long eventoId, String idExterno, String nombre, String tipo, Integer numero,
                           Instant inicio, Instant fin, Integer vueltasProgramadas, BigDecimal distancia,
                           String estado) {
    }
}

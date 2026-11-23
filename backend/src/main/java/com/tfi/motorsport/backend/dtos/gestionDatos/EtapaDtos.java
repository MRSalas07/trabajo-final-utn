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
 * DTOs del ABM de etapas (tramos de un rally).
 */
public final class EtapaDtos {

    private EtapaDtos() {
    }

    public record Request(
            @NotNull(message = "El evento es obligatorio") Long eventoId,
            @Min(0) @Max(999) Integer numero,
            @NotBlank @Size(max = 150) String nombre,
            @Min(1) @Max(10) Integer dia,
            @DecimalMin("0") BigDecimal distancia,
            Instant inicio,
            Instant fin,
            @Pattern(regexp = ESTADOS) String estado,
            /** true = es la clasificación general del rally (solo puede haber una por evento). */
            Boolean clasificacionGeneral
    ) {
    }

    public record Registro(Long id, Long eventoId, String idExterno, Integer numero, String nombre, Integer dia,
                           BigDecimal distancia, Instant inicio, Instant fin, String estado,
                           boolean clasificacionGeneral,
                           AuditoriaDto auditoria) {
    }
}
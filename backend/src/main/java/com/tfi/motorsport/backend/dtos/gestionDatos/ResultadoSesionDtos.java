package com.tfi.motorsport.backend.dtos.gestionDatos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

import static com.tfi.motorsport.backend.dtos.gestionDatos.ValidacionesAbm.ESTADOS_RESULTADO;

/**
 * DTOs del ABM de resultados de sesión (categorías de circuito: un piloto con su equipo).
 */
public final class ResultadoSesionDtos {

    private ResultadoSesionDtos() {
    }

    public record Request(
            @NotNull Long sesionId,
            @NotNull(message = "El piloto es obligatorio") Long pilotoId,
            @NotNull(message = "El equipo es obligatorio") Long equipoId,
            @Min(1) @Max(99) Integer posicion,
            @Min(0) @Max(999) Integer numero,
            @DecimalMin("0") BigDecimal puntos,
            @DecimalMin("0") BigDecimal tiempo,
            @DecimalMin("0") BigDecimal diferencia,
            @Min(0) Integer vueltas,
            @DecimalMin("0") BigDecimal mejorVuelta,
            @Pattern(regexp = ESTADOS_RESULTADO) String estado,
            @Size(max = 120) String motivoAbandono
    ) {
    }

    public record Registro(Long id, Long sesionId, Long pilotoId, String piloto, Long equipoId,
                           String equipo, Integer posicion, Integer numero, BigDecimal puntos,
                           BigDecimal tiempo, BigDecimal diferencia, Integer vueltas,
                           BigDecimal mejorVuelta, String estado, String motivoAbandono) {
    }
}


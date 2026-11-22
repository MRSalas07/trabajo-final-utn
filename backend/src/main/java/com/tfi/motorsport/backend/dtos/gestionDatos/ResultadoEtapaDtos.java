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
 * DTOs del ABM de resultados de etapa (rally: el resultado es de la tripulación).
 */
public final class ResultadoEtapaDtos {

    private ResultadoEtapaDtos() {
    }

    public record Request(
            @NotNull Long etapaId,
            @NotNull(message = "La tripulación es obligatoria") Long tripulacionId,
            @Min(1) @Max(999) Integer posicion,
            @DecimalMin("0") BigDecimal tiempo,
            @DecimalMin("0") BigDecimal diferencia,
            @DecimalMin("0") BigDecimal puntos,
            @Pattern(regexp = ESTADOS_RESULTADO) String estado,
            @Size(max = 120) String motivoAbandono
    ) {
    }

    public record Registro(Long id, Long etapaId, Long tripulacionId, String tripulacion,
                           Integer posicion, BigDecimal tiempo, BigDecimal diferencia,
                           BigDecimal puntos, String estado, String motivoAbandono) {
    }
}


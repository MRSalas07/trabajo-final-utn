package com.tfi.motorsport.backend.dtos.integracionAPI;

import jakarta.validation.constraints.*;

import java.time.Instant;

/**
 * Estado de una sincronización ejecutada en segundo plano.
 */
public record CargaSincronizacionDto (
    String id,
    String categoria,
    int anio,
    String estado,
    Instant creado,
    Instant finalizado,
    ResumenSincronizacion resumen,
    String error
) {
        public static final String EN_COLA = "EN_COLA";
        public static final String EN_PROCESO = "EN_PROCESO";
        public static final String COMPLETADO = "COMPLETADO";
        public static final String FALLIDO = "FALLIDO";

        /** Cuerpo de la petición para iniciar una sincronización. */
        public record SolicitudSincronizacion(
                @NotBlank @Pattern(regexp = "[A-Za-z0-9]{1,10}") String categoria,
                @NotNull @Min(1950) @Max(2100) Integer anio
        ) {
        }
}

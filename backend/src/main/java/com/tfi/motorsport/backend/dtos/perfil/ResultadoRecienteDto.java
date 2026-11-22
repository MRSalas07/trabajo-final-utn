package com.tfi.motorsport.backend.dtos.perfil;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Resultado final de un evento (interfaz ResultadoReciente del frontend).
 * fecha: inicio del evento en UTC; el frontend la formatea con la zona del usuario.
 */
public record ResultadoRecienteDto(
        Long eventoId,
        String evento,
        String categoria,
        Integer anio,
        Instant fecha,
        Integer posicion,
        String estado,
        BigDecimal puntos,
        String equipo,
        String piloto,
        String sesion
) {
}

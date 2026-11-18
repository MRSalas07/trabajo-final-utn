package com.tfi.motorsport.backend.dtos.equipo;

import java.math.BigDecimal;

/**
 * Resumen numérico de un equipo calculado con los resultados cargados.
 * eventos: eventos con resultado final (carrera o clasificación general del rally).
 * victorias / podios: posiciones 1 y 1 a 3 en esos resultados (cuentan todos los pilotos del equipo).
 * puntos: suma de carreras, sprints y clasificaciones generales.
 */
public record EstadisticasEquipoDto(
        int eventos,
        int victorias,
        int podios,
        BigDecimal puntos
) {
}

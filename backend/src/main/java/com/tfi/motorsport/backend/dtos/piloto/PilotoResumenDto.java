package com.tfi.motorsport.backend.dtos.piloto;

/**
 * Tarjeta de un piloto en el listado (interfaz PilotoResumen del frontend).
 * equipoId / equipoActual: la participación más reciente que cumple los filtros aplicados.
 */
public record PilotoResumenDto(
        Long id,
        String nombre,
        String apellido,
        String codigo,
        Integer numero,
        String nacionalidad,
        String pais,
        String fotoUrl,
        Long equipoId,
        String equipoActual
) {
}

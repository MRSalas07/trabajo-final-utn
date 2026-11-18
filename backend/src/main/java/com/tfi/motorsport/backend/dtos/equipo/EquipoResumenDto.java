package com.tfi.motorsport.backend.dtos.equipo;

/** Tarjeta de un equipo (interfaz EquipoResumen del frontend). */
public record EquipoResumenDto(
        Long id,
        String nombre,
        String nombreCorto,
        String nacionalidad,
        String logoUrl
) {
}

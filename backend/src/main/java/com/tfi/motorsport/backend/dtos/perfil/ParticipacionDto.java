package com.tfi.motorsport.backend.dtos.perfil;

/**
 * Participación de un piloto en un equipo durante una temporada (interfaz Participacion del frontend).
 * Se usa en la trayectoria del piloto y en el plantel del equipo.
 */
public record ParticipacionDto(
        Integer anio,
        String categoria,
        Long equipoId,
        String equipo,
        Long pilotoId,
        String piloto,
        Integer numero,
        String rol
) {
}

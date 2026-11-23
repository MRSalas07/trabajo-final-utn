package com.tfi.motorsport.backend.repositories.unificacion;

/**
 * Marca cargada en una participación (equipo_x_piloto.marca): con qué marca/motor corrió
 * un piloto para un equipo en una temporada. Ver {@code MarcasPorParticipacion}.
 */
public record MarcaParticipacion(Long pilotoId, Long equipoId, Integer anio, String marca) {
}

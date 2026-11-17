package com.tfi.motorsport.backend.dtos.piloto;

import java.math.BigDecimal;

/** Palmarés de un piloto en una categoría (interfaz Palmares del frontend). Los datos faltantes se envían en 0. */
public record PalmaresDto(
        String categoria,
        int carreras,
        int victorias,
        int podios,
        int poles,
        int campeonatos,
        BigDecimal puntosTotales,
        int abandonos
) {
}

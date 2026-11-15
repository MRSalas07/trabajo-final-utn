package com.tfi.motorsport.backend.dtos.resultados;

import java.math.BigDecimal;

public record ResultadoEtapaDto(
        Integer posicion,
        Integer numero,
        Long pilotoId,
        String piloto,
        Long copilotoId,
        String copiloto,
        Long equipoId,
        String equipo,
        BigDecimal tiempo,
        BigDecimal diferencia,
        BigDecimal puntos,
        String estado,
        String motivoAbandono
) {
}

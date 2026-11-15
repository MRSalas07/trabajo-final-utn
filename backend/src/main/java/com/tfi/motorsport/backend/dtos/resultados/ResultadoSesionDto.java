package com.tfi.motorsport.backend.dtos.resultados;

import java.math.BigDecimal;

public record ResultadoSesionDto(
        Integer posicion,
        Integer numero,
        Long pilotoId,
        String piloto,
        String codigo,
        Long equipoId,
        String equipo,
        BigDecimal puntos,
        BigDecimal tiempo,
        BigDecimal diferencia,
        Integer vueltas,
        BigDecimal mejorVuelta,
        String estado,
        String motivoAbandono) {
}



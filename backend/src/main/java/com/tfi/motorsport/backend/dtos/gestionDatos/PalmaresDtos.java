package com.tfi.motorsport.backend.dtos.gestionDatos;

import java.math.BigDecimal;

/**
 * DTO del palmarés. Es un dato DERIVADO (se recalcula a partir de los resultados),
 * por eso no tiene Request: no se edita a mano.
 */
public final class PalmaresDtos {

    private PalmaresDtos() {
    }

    public record Registro(Long id, Long pilotoId, String piloto, String categoria, Integer carreras,
                           Integer victorias, Integer podios, Integer poles, Integer campeonatos,
                           BigDecimal puntosTotales, Integer abandonos) {
    }
}
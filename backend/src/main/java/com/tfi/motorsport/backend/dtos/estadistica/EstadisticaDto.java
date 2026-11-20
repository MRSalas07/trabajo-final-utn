package com.tfi.motorsport.backend.dtos.estadistica;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class EstadisticaDto {
    public EstadisticaDto() {}

    /** Fila de la tabla de campeonato de pilotos (calculada a partir de los resultados). */
    public record FilaCampeonatoPiloto(
            int posicion,
            Long pilotoId,
            String piloto,
            String codigo,
            Long equipoId,
            String equipo,
            BigDecimal puntos,
            int victorias,
            int podios,
            int poles,
            int eventosDisputados,
            int abandonos,
            Integer mejorResultado,
            BigDecimal promedioPosicion,
            BigDecimal puntosPorEvento,
            /** Solo WRC: tramos ganados. */
            int victoriasEtapa
    ) {
    }

    public record FilaCampeonatoEquipo(
            int posicion,
            Long equipoId,
            String equipo,
            BigDecimal puntos,
            int victorias,
            int podios,
            List<String> pilotos
    ) {
    }

    public record Ronda(Long eventoId, Integer numeroFecha, String evento) {
    }

    public record GanadorEvento(
            Long eventoId,
            Integer numeroFecha,
            String evento,
            Instant fecha,
            Long pilotoId,
            String piloto,
            String equipo
    ) {
    }

    public record ResumenTemporada(
            Long temporadaId,
            String categoria,
            Integer anio,
            int eventosTotales,
            int eventosDisputados,
            FilaCampeonatoPiloto lider,
            FilaCampeonatoEquipo equipoLider,
            int ganadoresDistintos,
            List<GanadorEvento> ganadores
    ) {
    }
}

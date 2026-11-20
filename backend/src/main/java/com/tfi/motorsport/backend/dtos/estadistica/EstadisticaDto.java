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

    /** Serie de un piloto en el gráfico de evolución (un valor por ronda). */
    public record SerieEvolucion(
            Long pilotoId,
            String piloto,
            String codigo,
            List<BigDecimal> puntosAcumulados,
            List<Integer> posicionCampeonato,
            List<Integer> posicionEvento
    ) {
    }

    public record EvolucionCampeonato(List<Ronda> rondas, List<SerieEvolucion> series) {
    }

    public record PuntoTendencia(
            Integer numeroFecha,
            String evento,
            Integer posicion,
            String estado,
            BigDecimal puntos,
            /** Media móvil de 3 eventos de la posición final (suaviza resultados aislados). */
            BigDecimal mediaMovil
    ) {
    }

    /**
     * Tendencia de rendimiento de un piloto en una temporada.
     * pendiente: resultado de una regresión lineal posición vs. fecha.
     * Negativa = las posiciones bajan de número = el piloto MEJORA.
     */
    public record TendenciaPiloto(
            Long pilotoId,
            String piloto,
            Integer anio,
            List<PuntoTendencia> puntos,
            BigDecimal promedioPosicion,
            BigDecimal desviacionEstandar,
            BigDecimal pendiente,
            String interpretacion,
            BigDecimal tasaFinalizacion,
            Integer mejorResultado,
            Integer peorResultado
    ) {
    }

    public record ResumenComparado(
            Long pilotoId,
            String piloto,
            String equipo,
            BigDecimal puntos,
            int victorias,
            int podios,
            int poles,
            BigDecimal promedioPosicion,
            int eventosDisputados
    ) {
    }

    public record ComparativaPilotos(
            Integer anio,
            ResumenComparado a,
            ResumenComparado b,
            /** Veces que A terminó delante de B y viceversa (en eventos donde ambos clasificaron). */
            int carrerasDelanteA,
            int carrerasDelanteB,
            int clasificacionesDelanteA,
            int clasificacionesDelanteB,
            List<Ronda> rondas,
            /** Puntos acumulados de A menos los de B, ronda a ronda. */
            List<BigDecimal> diferenciaPuntos
    ) {
    }
}

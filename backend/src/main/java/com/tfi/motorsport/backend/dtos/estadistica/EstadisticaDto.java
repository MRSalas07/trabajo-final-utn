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
    /** Ranking de marcas o fabricantes de una temporada (Toyota, Hyundai, Ford, Chevrolet...). */
    public record FilaMarca(
            int posicion,
            String marca,
            BigDecimal puntos,
            int victorias,
            int podios,
            int eventosConResultados,
            Integer mejorResultado,
            List<String> equipos,
            int pilotos
    ) {
    }

    /** Puntos de un equipo o marca en una fecha. */
    public record PuntoGrupo(
            Integer numeroFecha,
            String evento,
            BigDecimal puntos,
            BigDecimal puntosAcumulados,
            /** Mejor posición final de alguno de sus pilotos en esa fecha. */
            Integer mejorPosicion,
            /** Media móvil de 3 fechas de los puntos obtenidos. */
            BigDecimal mediaMovilPuntos
    ) {
    }

    /**
     * Tendencia de rendimiento de un equipo o de una marca en una temporada.
     * pendiente: regresión lineal de los puntos por fecha. Positiva = suma cada vez más = MEJORA.
     */
    public record TendenciaGrupo(
            String tipo,
            String clave,
            String nombre,
            Integer anio,
            List<PuntoGrupo> fechas,
            BigDecimal promedioPuntos,
            BigDecimal pendiente,
            String interpretacion,
            int victorias,
            int podios,
            List<String> pilotos
    ) {
    }

    public record SerieGrupo(String clave, String nombre, List<BigDecimal> puntosAcumulados, List<BigDecimal> puntosPorFecha) {
    }

    /** Evolución ronda a ronda de varios equipos o marcas. */
    public record EvolucionGrupos(String tipo, List<Ronda> rondas, List<SerieGrupo> series) {
    }

    /** Resultado de un piloto o equipo en una temporada (fila de su historial). */
    public record TemporadaHistorica(
            Integer anio,
            Integer posicionCampeonato,
            int participantes,
            BigDecimal puntos,
            int victorias,
            int podios,
            int poles,
            int eventosDisputados,
            BigDecimal promedioPosicion,
            BigDecimal puntosPorEvento,
            /** Equipo (si es un piloto) o pilotos (si es un equipo). */
            String detalle
    ) {
    }

    /** Historial temporada por temporada de un piloto o equipo dentro de una categoría. */
    public record Historico(
            String tipo,
            Long id,
            String nombre,
            String categoria,
            List<TemporadaHistorica> temporadas,
            BigDecimal puntosTotales,
            int victoriasTotales,
            int podiosTotales,
            Integer mejorPosicionCampeonato,
            /** Variación de puntos por evento entre la primera y la última temporada (en %). */
            BigDecimal variacionPuntosPorEvento
    ) {
    }

    /** Dos pilotos comparados a lo largo de todas las temporadas cargadas de la categoría. */
    public record ComparativaHistorica(
            String categoria,
            List<Integer> anios,
            Historico a,
            Historico b,
            /** Posición en el campeonato de cada piloto por año (null = no corrió ese año). */
            List<Integer> posicionesA,
            List<Integer> posicionesB,
            List<BigDecimal> puntosA,
            List<BigDecimal> puntosB,
            int carrerasDelanteA,
            int carrerasDelanteB,
            int temporadasDelanteA,
            int temporadasDelanteB
    ) {
    }
}

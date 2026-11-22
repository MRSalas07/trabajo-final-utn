package com.tfi.motorsport.backend.services.estadisticas;

import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.*;
import com.tfi.motorsport.backend.services.estadisticas.MotorEstadistico.Participacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comparativas históricas: calcula el campeonato de cada temporada de una categoría por separado
 * y arma el historial de un piloto o equipo a lo largo de los años.
 */
public final class AnalisisHistorico {

    public static final String TIPO_PILOTO = "PILOTO";
    public static final String TIPO_EQUIPO = "EQUIPO";

    private AnalisisHistorico() {
    }

    private static TreeMap<Integer, List<Participacion>> agruparPorAnio(List<Participacion> participaciones) {
        return participaciones.stream().filter(participacion -> participacion.anio() != null)
                .collect(Collectors.groupingBy(Participacion::anio, TreeMap::new, Collectors.toList()));
    }

    private static Historico armarHistorico(String tipo, Long id, String nombre, String categoria,
                                            List<TemporadaHistorica> temporadas) {
        BigDecimal puntosTotales = temporadas.stream().map(TemporadaHistorica::puntos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Integer mejorPosicion = temporadas.stream().map(TemporadaHistorica::posicionCampeonato)
                .min(Integer::compare).orElse(null);
        return new Historico(tipo, id, nombre, categoria, temporadas, puntosTotales,
                temporadas.stream().mapToInt(TemporadaHistorica::victorias).sum(),
                temporadas.stream().mapToInt(TemporadaHistorica::podios).sum(),
                mejorPosicion, variacionPuntosPorEvento(temporadas));
    }

    /** Variación porcentual de los puntos por evento entre la primera y la última temporada. */
    private static BigDecimal variacionPuntosPorEvento(List<TemporadaHistorica> temporadas) {
        if (temporadas.size() < 2) {
            return null;
        }
        BigDecimal primera = temporadas.get(0).puntosPorEvento();
        BigDecimal ultima = temporadas.get(temporadas.size() - 1).puntosPorEvento();
        if (primera == null || ultima == null || primera.signum() <= 0) {
            return null;
        }
        return ultima.subtract(primera).multiply(BigDecimal.valueOf(100)).divide(primera, 1, RoundingMode.HALF_UP);
    }

    private static Map<Integer, TemporadaHistorica> indexarPorAnio(Historico historico) {
        Map<Integer, TemporadaHistorica> temporadasPorAnio = new HashMap<>();
        historico.temporadas().forEach(temporada -> temporadasPorAnio.put(temporada.anio(), temporada));
        return temporadasPorAnio;
    }
}

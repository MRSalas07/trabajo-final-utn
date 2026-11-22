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

    public static Historico historicoPiloto (List<Participacion> participacionesxCategoria, Long pilotoId,
                                             String categoria) {
        List<TemporadaHistorica> temporadas = new ArrayList<>();
        String nombre = null;
        for (Map.Entry<Integer, List<Participacion>> temporada : agruparPorAnio(participacionesxCategoria).entrySet()) {
            List<FilaCampeonatoPiloto> tabla = MotorEstadistico.campeonatoPilotos(temporada.getValue());
            Optional<FilaCampeonatoPiloto> filaDelPiloto = tabla.stream()
                    .filter(fila -> fila.pilotoId().equals(pilotoId)).findFirst();
            if (filaDelPiloto.isEmpty()) {
                continue;
            }
            FilaCampeonatoPiloto fila = filaDelPiloto.get();
            nombre = fila.piloto();
            temporadas.add(new TemporadaHistorica(temporada.getKey(), fila.posicion(), tabla.size(), fila.puntos(),
                    fila.victorias(), fila.podios(), fila.poles(), fila.eventosDisputados(), fila.promedioPosicion(),
                    fila.puntosPorEvento(), fila.equipo()));
        }
        return armarHistorico(TIPO_PILOTO, pilotoId, nombre, categoria, temporadas);
    }

    public static Historico historicoEquipo(List<Participacion> participacionesXCategoria, Long equipoId, String categoria) {
        List<TemporadaHistorica> temporadas = new ArrayList<>();
        String nombre = null;

        for (Map.Entry<Integer, List<Participacion>> temporada : agruparPorAnio(participacionesXCategoria).entrySet()) {
            List<FilaCampeonatoEquipo> tabla = MotorEstadistico.campeonatoEquipos(temporada.getValue());
            Optional<FilaCampeonatoEquipo> filaDelEquipo = tabla.stream()
                    .filter(fila -> fila.equipoId().equals(equipoId)).findFirst();
            if (filaDelEquipo.isEmpty()) {
                continue;
            }
            FilaCampeonatoEquipo fila = filaDelEquipo.get();
            nombre = fila.equipo();
            List<Participacion> disputadasDelEquipo = temporada.getValue().stream()
                    .filter(participacion -> equipoId.equals(participacion.equipoId()) && participacion.disputo())
                    .toList();
            int eventosDisputados = (int) disputadasDelEquipo.stream().map(Participacion::eventoId).distinct().count();
            List<Integer> posicionesTerminadas = disputadasDelEquipo.stream().filter(Participacion::finalizo)
                    .map(Participacion::posicionFinal).filter(Objects::nonNull).toList();

            temporadas.add(new TemporadaHistorica(temporada.getKey(), fila.posicion(), tabla.size(), fila.puntos(),
                    fila.victorias(), fila.podios(),
                    (int) disputadasDelEquipo.stream().filter(Participacion::pole).count(),
                    eventosDisputados,
                    MotorEstadistico.promedioDePosiciones(posicionesTerminadas),
                    eventosDisputados == 0 ? null
                            : fila.puntos().divide(BigDecimal.valueOf(eventosDisputados), 2, RoundingMode.HALF_UP),
                    String.join(", ", fila.pilotos())));
        }
        return armarHistorico(TIPO_EQUIPO, equipoId, nombre, categoria, temporadas);
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
        BigDecimal resultado = ultima.subtract(primera).multiply(BigDecimal.valueOf(100)).divide(primera, 1, RoundingMode.HALF_UP);
        return resultado;
    }

    private static Map<Integer, TemporadaHistorica> indexarPorAnio(Historico historico) {
        Map<Integer, TemporadaHistorica> temporadasPorAnio = new HashMap<>();
        historico.temporadas().forEach(temporada -> temporadasPorAnio.put(temporada.anio(), temporada));
        return temporadasPorAnio;
    }
}

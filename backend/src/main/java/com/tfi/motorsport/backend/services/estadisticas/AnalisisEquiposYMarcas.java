package com.tfi.motorsport.backend.services.estadisticas;

import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.*;
import com.tfi.motorsport.backend.services.estadisticas.MotorEstadistico.Participacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Rendimiento agregado por EQUIPO y por MARCA (fabricante), calculado con funciones puras.
 * La marca sale de equipo.nombre_corto: en una misma marca pueden convivir varios equipos.
 */
public final class AnalisisEquiposYMarcas {

    public static final String TIPO_EQUIPO = "EQUIPO";
    public static final String TIPO_MARCA = "MARCA";

    private static final int FECHAS_MEDIA_MOVIL = 3;
    private static final BigDecimal UMBRAL_DE_TENDENCIA = new BigDecimal("0.5");

    private AnalisisEquiposYMarcas() {
    }

    public static String claveEquipo(Participacion participacion) {
        return participacion.equipoId() != null ? String.valueOf(participacion.equipoId()) : null;
    }

    public static String claveMarca(Participacion participacion) {
        String marca = participacion.marca() != null ? participacion.marca() : participacion.equipo();
        return marca != null ? marca.trim().toUpperCase(Locale.ROOT) : null;
    }

    

    private static Function<Participacion, String> funcionDeClave(String tipo) {
        return TIPO_MARCA.equals(tipo) ? AnalisisEquiposYMarcas::claveMarca : AnalisisEquiposYMarcas::claveEquipo;
    }

    private static String interpretarPendiente(BigDecimal pendiente) {
        if (pendiente == null) {
            return "Datos insuficientes (se necesitan al menos 3 fechas)";
        }
        if (pendiente.compareTo(UMBRAL_DE_TENDENCIA) > 0) {
            return "Mejorando";
        }
        if (pendiente.compareTo(UMBRAL_DE_TENDENCIA.negate()) < 0) {
            return "Empeorando";
        }
        return "Estable";
    }

    private static BigDecimal mediaMovil(List<BigDecimal> puntosPorFecha) {
        List<BigDecimal> ultimas = puntosPorFecha.subList(
                Math.max(0, puntosPorFecha.size() - FECHAS_MEDIA_MOVIL), puntosPorFecha.size());
        return ultimas.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(ultimas.size()), 2, RoundingMode.HALF_UP);
    }

    private static Integer mejorPosicion(List<Participacion> participaciones) {
        return participaciones.stream().filter(Participacion::finalizo).map(Participacion::posicionFinal)
                .filter(Objects::nonNull).min(Integer::compare).orElse(null);
    }

    private static BigDecimal puntosFinales(SerieGrupo serie) {
        return serie.puntosAcumulados().isEmpty() ? BigDecimal.ZERO
                : serie.puntosAcumulados().get(serie.puntosAcumulados().size() - 1);
    }

    /** Nombre visible de la marca: la variante más usada en los datos. */
    private static String nombreDeLaMarca(List<Participacion> participaciones) {
        return participaciones.stream()
                .map(participacion -> participacion.marca() != null ? participacion.marca() : participacion.equipo())
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("Sin marca");
    }

    private static Map<String, List<Participacion>> agruparPor(List<Participacion> participaciones,
                                                               Function<Participacion, String> obtenerClave) {
        Map<String, List<Participacion>> grupos = new LinkedHashMap<>();
        for (Participacion participacion : participaciones) {
            String clave = obtenerClave.apply(participacion);
            if (clave != null) {
                grupos.computeIfAbsent(clave, claveNueva -> new ArrayList<>()).add(participacion);
            }
        }
        return grupos;
    }
}
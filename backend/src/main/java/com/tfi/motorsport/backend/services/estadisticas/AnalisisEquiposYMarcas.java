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

    public static List<FilaMarca> rankingMarcas(List<Participacion> participaciones) {
        Map<String, List<Participacion>> participacionesPorMarca = agruparPor(participaciones, AnalisisEquiposYMarcas::claveMarca);
        List<FilaMarca> filasSinPosicion = new ArrayList<>();

        for (List<Participacion> participacionesDeLaMarca : participacionesPorMarca.values()) {
            List<Participacion> disputadas = participacionesDeLaMarca.stream().filter(Participacion::disputo).toList();
            if (disputadas.isEmpty()) {
                continue;
            }
            Set<String> equipos = participacionesDeLaMarca.stream().map(Participacion::equipo).filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            filasSinPosicion.add(new FilaMarca(0, nombreDeLaMarca(participacionesDeLaMarca),
                    MotorEstadistico.sumarPuntos(participacionesDeLaMarca),
                    (int) participacionesDeLaMarca.stream().filter(Participacion::gano).count(),
                    (int) participacionesDeLaMarca.stream().filter(Participacion::podio).count(),
                    (int) disputadas.stream().map(Participacion::eventoId).distinct().count(),
                    mejorPosicion(disputadas),
                    List.copyOf(equipos),
                    (int) disputadas.stream().map(Participacion::pilotoId).distinct().count()));
        }

        filasSinPosicion.sort(Comparator.comparing(FilaMarca::puntos).reversed()
                .thenComparing(Comparator.comparingInt(FilaMarca::victorias).reversed()));

        List<FilaMarca> ranking = new ArrayList<>();
        for (int indice = 0; indice < filasSinPosicion.size(); indice++) {
            FilaMarca fila = filasSinPosicion.get(indice);
            ranking.add(new FilaMarca(indice + 1, fila.marca(), fila.puntos(), fila.victorias(), fila.podios(),
                    fila.eventosConResultados(), fila.mejorResultado(), fila.equipos(), fila.pilotos()));
        }
        return ranking;
    }

    /** Puntos por fecha de un equipo o marca, con acumulado, media móvil y regresión lineal. */
    public static TendenciaGrupo tendencia(List<Participacion> participaciones, String tipo, String clave) {
        Function<Participacion, String> obtenerClave = funcionDeClave(tipo);
        String claveBuscada = TIPO_MARCA.equals(tipo) ? clave.trim().toUpperCase(Locale.ROOT) : clave;
        List<Participacion> delGrupo = participaciones.stream()
                .filter(participacion -> claveBuscada.equals(obtenerClave.apply(participacion))).toList();
        List<Ronda> rondas = MotorEstadistico.rondas(participaciones);

        List<PuntoGrupo> fechas = new ArrayList<>();
        List<double[]> puntosParaRegresion = new ArrayList<>();
        List<BigDecimal> puntosPorFecha = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO;

        for (int indiceRonda = 0; indiceRonda < rondas.size(); indiceRonda++) {
            Ronda ronda = rondas.get(indiceRonda);
            List<Participacion> enLaFecha = delGrupo.stream()
                    .filter(participacion -> participacion.eventoId().equals(ronda.eventoId())).toList();
            if (enLaFecha.stream().noneMatch(Participacion::disputo)) {
                continue;
            }
            BigDecimal puntos = MotorEstadistico.sumarPuntos(enLaFecha);
            acumulado = acumulado.add(puntos);
            puntosPorFecha.add(puntos);
            puntosParaRegresion.add(new double[]{indiceRonda + 1, puntos.doubleValue()});
            fechas.add(new PuntoGrupo(ronda.numeroFecha(), ronda.evento(), puntos, acumulado,
                    mejorPosicion(enLaFecha), mediaMovil(puntosPorFecha)));
        }

        BigDecimal pendiente = MotorEstadistico.pendienteDeRegresionLineal(puntosParaRegresion);
        BigDecimal promedio = fechas.isEmpty() ? null
                : acumulado.divide(BigDecimal.valueOf(fechas.size()), 2, RoundingMode.HALF_UP);
        String nombre = delGrupo.isEmpty() ? clave
                : TIPO_MARCA.equals(tipo) ? nombreDeLaMarca(delGrupo) : delGrupo.get(0).equipo();
        List<String> pilotos = delGrupo.stream().filter(Participacion::disputo).map(Participacion::piloto)
                .distinct().toList();

        return new TendenciaGrupo(tipo, clave, nombre, delGrupo.isEmpty() ? null : delGrupo.get(0).anio(),
                fechas, promedio, pendiente, interpretarPendiente(pendiente),
                (int) delGrupo.stream().filter(Participacion::gano).count(),
                (int) delGrupo.stream().filter(Participacion::podio).count(), pilotos);
    }

    /** Puntos acumulados ronda a ronda de los {@code top} equipos o marcas con más puntos. */
    public static EvolucionGrupos evolucion(List<Participacion> participaciones, String tipo, int top) {
        List<Ronda> rondas = MotorEstadistico.rondas(participaciones);
        Map<String, List<Participacion>> grupos = agruparPor(participaciones, funcionDeClave(tipo));

        List<SerieGrupo> series = new ArrayList<>();
        for (Map.Entry<String, List<Participacion>> grupo : grupos.entrySet()) {
            List<BigDecimal> puntosPorFecha = new ArrayList<>();
            List<BigDecimal> puntosAcumulados = new ArrayList<>();
            BigDecimal acumulado = BigDecimal.ZERO;
            for (Ronda ronda : rondas) {
                BigDecimal puntos = MotorEstadistico.sumarPuntos(grupo.getValue().stream()
                        .filter(participacion -> participacion.eventoId().equals(ronda.eventoId())).toList());
                acumulado = acumulado.add(puntos);
                puntosPorFecha.add(puntos);
                puntosAcumulados.add(acumulado);
            }
            String nombre = TIPO_MARCA.equals(tipo) ? nombreDeLaMarca(grupo.getValue()) : grupo.getValue().get(0).equipo();
            series.add(new SerieGrupo(grupo.getKey(), nombre, puntosAcumulados, puntosPorFecha));
        }

        series.sort(Comparator.comparing(AnalisisEquiposYMarcas::puntosFinales).reversed());
        return new EvolucionGrupos(tipo, rondas, series.stream().limit(Math.max(1, top)).toList());
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
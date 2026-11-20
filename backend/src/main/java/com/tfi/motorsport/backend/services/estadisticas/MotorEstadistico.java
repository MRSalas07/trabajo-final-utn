package com.tfi.motorsport.backend.services.estadisticas;


import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.*;

import com.tfi.motorsport.backend.model.Catalogo;
import com.tfi.motorsport.backend.model.Catalogo.*;
import com.tfi.motorsport.backend.repositories.unificacion.FilaResultado;
import org.springframework.boot.SpringApplication;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

//Participacion
// consolidar
//rondas
//campeonatoPilotos
//CampeonaEquipo
//ganadores
//utilidades
public final class MotorEstadistico {
    private static final String TIPO_GENERAL = "GENERAL";
    private static final String TIPO_ETAPA = "ETAPA";

    /** Cantidad de puestos que se cuentan para el desempate (de 1º a 40º). */
    private static final int MAX_PUESTOS_CONTADOS = 40;

    private MotorEstadistico() {
    }

    /** Un piloto en un evento, con todo lo necesario para las estadísticas. */
    public record Participacion(
            Long eventoId,
            Integer numeroFecha,
            String evento,
            Instant fecha,
            Long temporadaId,
            Integer anio,
            Long pilotoId,
            String piloto,
            String codigo,
            Long equipoId,
            String equipo,
            Integer posicionFinal,
            String estadoFinal,
            BigDecimal puntos,
            Integer posicionClasificacion,
            int victoriasEtapa,
            boolean disputo
    ) {
        public boolean finalizo() {
            return disputo && EstadoResultado.FINALIZADO.equals(estadoFinal);
        }

        /** Victoria/podio: posición final 1 o ≤ 3, sin abandono ni descalificación. */
        public boolean gano() {

            return valida() && posicionFinal == 1;
        }

        public boolean podio() {

            return valida() && posicionFinal <= 3;
        }

        public boolean pole() {

            return posicionClasificacion != null && posicionClasificacion == 1;
        }

        public boolean abandono() {
            return disputo && EstadoResultado.ABANDONO.equals(estadoFinal);
        }

        private boolean valida() {
            return disputo && posicionFinal != null
                    && !EstadoResultado.ABANDONO.equals(estadoFinal)
                    && !EstadoResultado.DESCALIFICADO.equals(estadoFinal);
        }
    }

    public static List<Participacion> consolidar(List<FilaResultado> filas) {
        Map<String, List<FilaResultado>> filasPorPilotoYEvento = new LinkedHashMap<>();
        for (FilaResultado fila : filas) {
            String clave = fila.eventoId() + "-" + fila.pilotoId();
            filasPorPilotoYEvento.computeIfAbsent(clave, k -> new ArrayList<>()).add(fila);
        }

        List<Participacion> participaciones = new ArrayList<>();
        for (List<FilaResultado> filasDelPilotoEnElEvento : filasPorPilotoYEvento.values()) {
            FilaResultado filaPrincipal = filasDelPilotoEnElEvento.stream()
                    .filter(fila -> TipoSesion.CARRERA.equals(fila.tipo()) || TIPO_GENERAL.equals(fila.tipo()))
                    .findFirst().orElse(null);
            // De la fila de referencia salen los datos de texto (nombres de piloto, equipo y evento)
            FilaResultado filaReferencia = filaPrincipal != null ? filaPrincipal : filasDelPilotoEnElEvento.get(0);

            // Los tramos de rally no dan puntos de campeonato, por eso se excluyen
            BigDecimal puntosDelEvento = filasDelPilotoEnElEvento.stream()
                    .filter(fila -> !TIPO_ETAPA.equals(fila.tipo()))
                    .map(FilaResultado::puntos).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Integer posicionEnClasificacion = filasDelPilotoEnElEvento.stream()
                    .filter(fila -> TipoSesion.CLASIFICACION.equals(fila.tipo()))
                    .map(FilaResultado::posicion).filter(Objects::nonNull)
                    .findFirst().orElse(null);
            int tramosGanados = (int) filasDelPilotoEnElEvento.stream()
                    .filter(fila -> TIPO_ETAPA.equals(fila.tipo()) && Integer.valueOf(1).equals(fila.posicion()))
                    .count();

            participaciones.add(new Participacion(
                    filaReferencia.eventoId(), filaReferencia.numeroFecha(), filaReferencia.eventoNombre(),
                    filaReferencia.eventoInicio(), filaReferencia.temporadaId(), filaReferencia.anio(),
                    filaReferencia.pilotoId(), filaReferencia.nombreCompleto(), filaReferencia.pilotoCodigo(),
                    filaReferencia.equipoId(), filaReferencia.equipoNombre(),
                    filaPrincipal != null ? filaPrincipal.posicion() : null,
                    filaPrincipal != null ? filaPrincipal.estado() : null,
                    puntosDelEvento, posicionEnClasificacion, tramosGanados, filaPrincipal != null));
        }
        return participaciones;
    }

    /** Rondas disputadas (con al menos un resultado principal), en orden cronológico. */
    public static List<Ronda> rondas(List<Participacion> participaciones) {
        Map<Long, Participacion> primeraParticipacionPorEvento = new LinkedHashMap<>();
        participaciones.stream().filter(Participacion::disputo)
                .sorted(ordenCronologico())
                .forEach(participacion ->
                        primeraParticipacionPorEvento.putIfAbsent(participacion.eventoId(), participacion));
        return primeraParticipacionPorEvento.values().stream()
                .map(participacion -> new Ronda(
                        participacion.eventoId(), participacion.numeroFecha(), participacion.evento()))
                .toList();
    }

    public static List<FilaCampeonatoPiloto> campeonatoPilotos(List<Participacion> participaciones) {
        Map<Long, List<Participacion>> participacionesPorPiloto = agruparPor(participaciones, Participacion::pilotoId);
        List<FilaCampeonatoPiloto> filasSinPosicion = new ArrayList<>();

        for (List<Participacion> participacionesDelPiloto : participacionesPorPiloto.values()) {
            List<Participacion> participacionesDisputadas = participacionesDelPiloto.stream()
                    .filter(Participacion::disputo).toList();
            if (participacionesDisputadas.isEmpty()) {
                continue; // solo clasificó o solo figura en tramos: no entra al campeonato
            }
            // Se usa la más reciente para mostrar el equipo actual (puede haber cambiado en el año)
            Participacion participacionMasReciente = participacionesDisputadas.stream()
                    .max(ordenCronologico()).orElseThrow();
            BigDecimal puntosTotales = sumarPuntos(participacionesDelPiloto);
            // Solo carreras terminadas: un abandono no debe arruinar el promedio ni el mejor resultado
            List<Integer> posicionesEnCarrerasTerminadas = participacionesDisputadas.stream()
                    .filter(Participacion::finalizo)
                    .map(Participacion::posicionFinal).filter(Objects::nonNull).toList();

            filasSinPosicion.add(new FilaCampeonatoPiloto(
                    0, // la posición real se asigna después de ordenar
                    participacionMasReciente.pilotoId(), participacionMasReciente.piloto(),
                    participacionMasReciente.codigo(), participacionMasReciente.equipoId(),
                    participacionMasReciente.equipo(), puntosTotales,
                    (int) participacionesDelPiloto.stream().filter(Participacion::gano).count(),
                    (int) participacionesDelPiloto.stream().filter(Participacion::podio).count(),
                    (int) participacionesDelPiloto.stream().filter(Participacion::pole).count(),
                    participacionesDisputadas.size(),
                    (int) participacionesDelPiloto.stream().filter(Participacion::abandono).count(),
                    posicionesEnCarrerasTerminadas.stream().min(Integer::compare).orElse(null),
                    promedioDePosiciones(posicionesEnCarrerasTerminadas),
                    puntosTotales.divide(BigDecimal.valueOf(participacionesDisputadas.size()), 2, RoundingMode.HALF_UP),
                    participacionesDelPiloto.stream().mapToInt(Participacion::victoriasEtapa).sum()));
        }

        // Orden: puntos y, en caso de empate, "count-back" (más victorias, luego más 2dos puestos, etc.)
        Map<Long, int[]> puestosPorPiloto = contarPuestosPorPiloto(participaciones);
        filasSinPosicion.sort(Comparator.comparing(FilaCampeonatoPiloto::puntos).reversed()
                .thenComparing((filaA, filaB) -> compararPorPuestosObtenidos(
                        puestosPorPiloto.get(filaB.pilotoId()), puestosPorPiloto.get(filaA.pilotoId())))
                .thenComparing(FilaCampeonatoPiloto::piloto, Comparator.nullsLast(String::compareTo)));

        // Los records son inmutables: para asignar la posición se crea cada fila de nuevo
        List<FilaCampeonatoPiloto> tabla = new ArrayList<>();
        for (int indice = 0; indice < filasSinPosicion.size(); indice++) {
            FilaCampeonatoPiloto fila = filasSinPosicion.get(indice);
            tabla.add(new FilaCampeonatoPiloto(indice + 1, fila.pilotoId(), fila.piloto(), fila.codigo(),
                    fila.equipoId(), fila.equipo(), fila.puntos(), fila.victorias(), fila.podios(), fila.poles(),
                    fila.eventosDisputados(), fila.abandonos(), fila.mejorResultado(), fila.promedioPosicion(),
                    fila.puntosPorEvento(), fila.victoriasEtapa()));
        }
        return tabla;
    }

    /** Campeonato de equipos = suma de los puntos de sus pilotos en cada evento. */
    public static List<FilaCampeonatoEquipo> campeonatoEquipos(List<Participacion> participaciones) {
        Map<Long, List<Participacion>> participacionesPorEquipo = agruparPor(
                participaciones.stream().filter(participacion -> participacion.equipoId() != null).toList(),
                Participacion::equipoId);
        List<FilaCampeonatoEquipo> filasSinPosicion = new ArrayList<>();

        for (List<Participacion> participacionesDelEquipo : participacionesPorEquipo.values()) {
            Set<String> nombresDePilotos = participacionesDelEquipo.stream()
                    .filter(Participacion::disputo).map(Participacion::piloto)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (nombresDePilotos.isEmpty()) {
                continue;
            }
            filasSinPosicion.add(new FilaCampeonatoEquipo(0,
                    participacionesDelEquipo.get(0).equipoId(), participacionesDelEquipo.get(0).equipo(),
                    sumarPuntos(participacionesDelEquipo),
                    (int) participacionesDelEquipo.stream().filter(Participacion::gano).count(),
                    (int) participacionesDelEquipo.stream().filter(Participacion::podio).count(),
                    List.copyOf(nombresDePilotos)));
        }

        filasSinPosicion.sort(Comparator.comparing(FilaCampeonatoEquipo::puntos).reversed()
                .thenComparing(Comparator.comparingInt(FilaCampeonatoEquipo::victorias).reversed()));

        List<FilaCampeonatoEquipo> tabla = new ArrayList<>();
        for (int indice = 0; indice < filasSinPosicion.size(); indice++) {
            FilaCampeonatoEquipo fila = filasSinPosicion.get(indice);
            tabla.add(new FilaCampeonatoEquipo(indice + 1, fila.equipoId(), fila.equipo(), fila.puntos(),
                    fila.victorias(), fila.podios(), fila.pilotos()));
        }
        return tabla;
    }

    // =================================================================== ganadores

    static BigDecimal sumarPuntos(List<Participacion> participaciones) {
        return participaciones.stream().map(Participacion::puntos).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Promedio con 2 decimales; null si no hay datos (para distinguir "sin datos" de "promedio 0"). */
    static BigDecimal promedioDePosiciones(List<Integer> posiciones) {
        if (posiciones == null || posiciones.isEmpty()) {
            return null;
        }
        return BigDecimal.valueOf(posiciones.stream().mapToInt(Integer::intValue).average().orElse(0))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Para cada piloto, cuántas veces terminó en cada puesto.
     * El arreglo se lee así: posición 0 = veces que fue 1º, posición 1 = veces que fue 2º, etc.
     */
    private static Map<Long, int[]> contarPuestosPorPiloto(List<Participacion> participaciones) {
        Map<Long, int[]> puestosPorPiloto = new HashMap<>();
        for (Participacion participacion : participaciones) {
            int[] puestosDelPiloto = puestosPorPiloto.computeIfAbsent(
                    participacion.pilotoId(), clave -> new int[MAX_PUESTOS_CONTADOS]);
            Integer puesto = participacion.posicionFinal();
            if (participacion.disputo() && puesto != null && puesto >= 1 && puesto <= MAX_PUESTOS_CONTADOS) {
                puestosDelPiloto[puesto - 1]++;
            }
        }
        return puestosPorPiloto;
    }

    /**
     * Desempate "count-back" de la FIA: compara cuántas veces terminó cada piloto 1º, luego 2º, etc.
     * Devuelve positivo si A tiene más puestos altos que B, negativo si B tiene más, y 0 si son iguales.
     */
    private static int compararPorPuestosObtenidos(int[] puestosDeA, int[] puestosDeB) {
        if (puestosDeA == null || puestosDeB == null) {
            return 0;
        }
        for (int puesto = 0; puesto < puestosDeA.length; puesto++) {
            if (puestosDeA[puesto] != puestosDeB[puesto]) {
                return Integer.compare(puestosDeA[puesto], puestosDeB[puesto]);
            }
        }
        return 0;
    }

    private static <K> Map<K, List<Participacion>> agruparPor(List<Participacion> participaciones,
                                                              Function<Participacion, K> obtenerClave) {
        Map<K, List<Participacion>> grupos = new LinkedHashMap<>();
        participaciones.forEach(participacion -> grupos
                .computeIfAbsent(obtenerClave.apply(participacion), clave -> new ArrayList<>())
                .add(participacion));
        return grupos;
    }

    static Comparator<Participacion> ordenCronologico() {
        return Comparator.comparing(Participacion::fecha, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Participacion::numeroFecha, Comparator.nullsLast(Comparator.naturalOrder()));
    }


}

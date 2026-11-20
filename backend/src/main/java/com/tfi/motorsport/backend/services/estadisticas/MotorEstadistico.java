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

    /** Pendiente a partir de la cual se considera que un piloto mejora o empeora (en puestos por fecha). */
    private static final BigDecimal UMBRAL_DE_TENDENCIA = new BigDecimal("0.15");

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

    public static List<GanadorEvento> ganadores(List<Participacion> participaciones) {
        return participaciones.stream().filter(Participacion::gano)
                .sorted(ordenCronologico())
                .map(p -> new GanadorEvento(p.eventoId(), p.numeroFecha(), p.evento(), p.fecha(),
                        p.pilotoId(), p.piloto(), p.equipo()))
                .toList();
    }

    /**
     * Evolución ronda a ronda: puntos acumulados y posición en el campeonato luego de cada fecha.
     *
     * @param pilotoIds pilotos a incluir; si está vacío se toman los {@code topN} del campeonato final
     */
    public static EvolucionCampeonato evolucion(List<Participacion> participaciones, List<Long> pilotoIds, int topN) {
        List<Ronda> rondas = rondas(participaciones);
        List<Long> pilotosSeleccionados = (pilotoIds == null || pilotoIds.isEmpty())
                ? campeonatoPilotos(participaciones).stream()
                  .limit(topN).map(FilaCampeonatoPiloto::pilotoId).toList()
                : pilotoIds;

        Map<String, Participacion> participacionPorEventoYPiloto = indexarPorEventoYPiloto(participaciones);
        Set<Long> todosLosPilotos = participaciones.stream().map(Participacion::pilotoId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, BigDecimal> puntosAcumulados = new HashMap<>();
        Map<Long, List<BigDecimal>> seriesDePuntos = new LinkedHashMap<>();
        Map<Long, List<Integer>> seriesDePosicionEnCampeonato = new LinkedHashMap<>();
        Map<Long, List<Integer>> seriesDePosicionEnEvento = new LinkedHashMap<>();
        pilotosSeleccionados.forEach(pilotoId -> {
            seriesDePuntos.put(pilotoId, new ArrayList<>());
            seriesDePosicionEnCampeonato.put(pilotoId, new ArrayList<>());
            seriesDePosicionEnEvento.put(pilotoId, new ArrayList<>());
        });

        for (Ronda ronda : rondas) {
            // Se acumulan los puntos de TODOS los pilotos (no solo los graficados): hace falta para
            // saber en qué posición del campeonato quedó cada uno después de esta fecha
            for (Long pilotoId : todosLosPilotos) {
                Participacion participacion = participacionPorEventoYPiloto.get(claveEventoYPiloto(ronda.eventoId(), pilotoId));
                puntosAcumulados.merge(pilotoId, puntosOCero(participacion), BigDecimal::add);
            }
            List<Long> ordenDelCampeonato = puntosAcumulados.entrySet().stream()
                    .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                    .map(Map.Entry::getKey).toList();

            for (Long pilotoId : pilotosSeleccionados) {
                Participacion participacion = participacionPorEventoYPiloto.get(claveEventoYPiloto(ronda.eventoId(), pilotoId));
                seriesDePuntos.get(pilotoId).add(puntosAcumulados.getOrDefault(pilotoId, BigDecimal.ZERO));
                int indiceEnElOrden = ordenDelCampeonato.indexOf(pilotoId);
                seriesDePosicionEnCampeonato.get(pilotoId).add(indiceEnElOrden >= 0 ? indiceEnElOrden + 1 : null);
                seriesDePosicionEnEvento.get(pilotoId).add(
                        participacion != null && participacion.disputo() ? participacion.posicionFinal() : null);
            }
        }

        // Para mostrar nombre y código se usa la participación más reciente de cada piloto
        Map<Long, Participacion> participacionMasRecientePorPiloto = new HashMap<>();
        participaciones.stream().sorted(ordenCronologico())
                .forEach(participacion -> participacionMasRecientePorPiloto.put(participacion.pilotoId(), participacion));

        List<SerieEvolucion> series = pilotosSeleccionados.stream()
                .filter(participacionMasRecientePorPiloto::containsKey)
                .map(pilotoId -> new SerieEvolucion(
                        pilotoId,
                        participacionMasRecientePorPiloto.get(pilotoId).piloto(),
                        participacionMasRecientePorPiloto.get(pilotoId).codigo(),
                        seriesDePuntos.get(pilotoId),
                        seriesDePosicionEnCampeonato.get(pilotoId),
                        seriesDePosicionEnEvento.get(pilotoId)))
                .toList();
        return new EvolucionCampeonato(rondas, series);
    }


    public static TendenciaPiloto tendenciadePiloto(List<Participacion> participaciones, Long pilotoId) {
        List<Ronda> rondas = rondas(participaciones);
        Map<Long, Participacion> participacionPorEvento = participaciones.stream()
                .filter(participacion -> participacion.pilotoId().equals(pilotoId))
                .collect(Collectors.toMap(Participacion::eventoId, participacion -> participacion,
                        (primera, repetida) -> primera));
        Participacion cualquierParticipacion = participacionPorEvento.values().stream().findFirst().orElse(null);

        List<PuntoTendencia> puntos = new ArrayList<>();
        List<double[]> puntosParaRegresion = new ArrayList<>(); // cada punto: {número de ronda, posición}
        List<Integer> posicionesTerminadas = new ArrayList<>();
        int eventosDisputados = 0;
        int eventosFinalizados = 0;

        for (int indiceRonda = 0; indiceRonda < rondas.size(); indiceRonda++) {
            Ronda ronda = rondas.get(indiceRonda);
            Participacion participacion = participacionPorEvento.get(ronda.eventoId());
            if (participacion == null || !participacion.disputo()) {
                continue;
            }
            eventosDisputados++;
            // Solo las carreras terminadas entran al cálculo: un abandono no es una "posición"
            Integer posicionTerminada = participacion.finalizo() ? participacion.posicionFinal() : null;
            if (posicionTerminada != null) {
                eventosFinalizados++;
                puntosParaRegresion.add(new double[]{indiceRonda + 1, posicionTerminada});
                posicionesTerminadas.add(posicionTerminada);
            }
            // Media móvil: promedio de las últimas 3 posiciones terminadas hasta esta ronda
            List<Integer> ultimasTres = posicionesTerminadas.subList(
                    Math.max(0, posicionesTerminadas.size() - 3), posicionesTerminadas.size());
            puntos.add(new PuntoTendencia(ronda.numeroFecha(), ronda.evento(), participacion.posicionFinal(),
                    participacion.estadoFinal(), participacion.puntos(),
                    ultimasTres.isEmpty() ? null : promedioDePosiciones(ultimasTres)));
        }

        List<Integer> posiciones = puntosParaRegresion.stream().map(punto -> (int) punto[1]).toList();
        BigDecimal pendiente = pendienteDeRegresionLineal(puntosParaRegresion);
        BigDecimal tasaDeFinalizacion = eventosDisputados == 0 ? null
                : BigDecimal.valueOf(eventosFinalizados * 100.0 / eventosDisputados).setScale(1, RoundingMode.HALF_UP);

        return new TendenciaPiloto(pilotoId,
                cualquierParticipacion != null ? cualquierParticipacion.piloto() : null,
                cualquierParticipacion != null ? cualquierParticipacion.anio() : null,
                puntos, promedioDePosiciones(posiciones), desviacionEstandar(posiciones), pendiente,
                interpretarPendiente(pendiente), tasaDeFinalizacion,
                posiciones.stream().min(Integer::compare).orElse(null),
                posiciones.stream().max(Integer::compare).orElse(null));
    }

    private static String interpretarPendiente(BigDecimal pendiente) {
        if (pendiente == null) {
            return "Datos insuficientes (se necesitan al menos 3 resultados)";
        }
        if (pendiente.compareTo(UMBRAL_DE_TENDENCIA.negate()) < 0) {
            return "Mejorando";
        }
        if (pendiente.compareTo(UMBRAL_DE_TENDENCIA) > 0) {
            return "Empeorando";
        }
        return "Estable";
    }



    public static ComparativaPilotos comparativaPilotos(List<Participacion> participaciones, Long pilotoAId, Long pilotoBId) {
        List<FilaCampeonatoPiloto> campeonato = campeonatoPilotos(participaciones);
        List<Ronda> rondas = rondas(participaciones);
        Map<String, Participacion> participacionPorEventoYPiloto = indexarPorEventoYPiloto(participaciones);

        int carrerasDelanteA = 0;
        int carrerasDelanteB = 0;
        int clasificacionesDelanteA = 0;
        int clasificacionesDelanteB = 0;
        BigDecimal puntosAcumuladosA = BigDecimal.ZERO;
        BigDecimal puntosAcumuladosB = BigDecimal.ZERO;
        List<BigDecimal> diferenciaDePuntos = new ArrayList<>();

        for (Ronda ronda : rondas) {
            Participacion deA = participacionPorEventoYPiloto.get(claveEventoYPiloto(ronda.eventoId(), pilotoAId));
            Participacion deB = participacionPorEventoYPiloto.get(claveEventoYPiloto(ronda.eventoId(), pilotoBId));

            int duelo = calculoTerminoPorDelante(deA, deB);
            if (duelo > 0) carrerasDelanteA++;
            else if (duelo < 0) carrerasDelanteB++;

            if (deA != null && deB != null
                    && deA.posicionClasificacion() != null && deB.posicionClasificacion() != null) {
                if (deA.posicionClasificacion() < deB.posicionClasificacion()) clasificacionesDelanteA++;
                else if (deB.posicionClasificacion() < deA.posicionClasificacion()) clasificacionesDelanteB++;
            }

            puntosAcumuladosA = puntosAcumuladosA.add(puntosOCero(deA));
            puntosAcumuladosB = puntosAcumuladosB.add(puntosOCero(deB));
            diferenciaDePuntos.add(puntosAcumuladosA.subtract(puntosAcumuladosB));
        }

        Integer anio = participaciones.isEmpty() ? null : participaciones.get(0).anio();
        return new ComparativaPilotos(anio,
                resumenComparado(campeonato, pilotoAId, participaciones),
                resumenComparado(campeonato, pilotoBId, participaciones),
                carrerasDelanteA, carrerasDelanteB, clasificacionesDelanteA, clasificacionesDelanteB,
                rondas, diferenciaDePuntos);
    }

    private static ResumenComparado resumenComparado(List<FilaCampeonatoPiloto> campeonato, Long pilotoId,
                                                     List<Participacion> participaciones) {
        return campeonato.stream().filter(fila -> fila.pilotoId().equals(pilotoId)).findFirst()
                .map(fila -> new ResumenComparado(fila.pilotoId(), fila.piloto(), fila.equipo(), fila.puntos(),
                        fila.victorias(), fila.podios(), fila.poles(), fila.promedioPosicion(),
                        fila.eventosDisputados()))
                .orElseGet(() -> {
                    // El piloto no está en la tabla (solo clasificó o no corrió): se devuelve con ceros
                    Participacion cualquiera = participaciones.stream()
                            .filter(participacion -> participacion.pilotoId().equals(pilotoId))
                            .findFirst().orElse(null);
                    return new ResumenComparado(pilotoId,
                            cualquiera != null ? cualquiera.piloto() : null,
                            cualquiera != null ? cualquiera.equipo() : null,
                            BigDecimal.ZERO, 0, 0, 0, null, 0);
                });
    }

    /**
     * Duelo directo en un evento: positivo si A terminó delante de B, negativo si B delante de A,
     * 0 si no se puede decidir (alguno no corrió o no tiene posición).
     * En esta versión compara directamente la posición final; en el PR #19 se mejora para que
     * quien termina la carrera quede delante de quien abandona.
     */
    static int calculoTerminoPorDelante(Participacion participacionA, Participacion participacionB) {
        if (participacionA == null || participacionB == null
                || !participacionA.disputo() || !participacionB.disputo()
                || participacionA.posicionFinal() == null || participacionB.posicionFinal() == null) {
            return 0;
        }
        return Integer.compare(participacionB.posicionFinal(), participacionA.posicionFinal());
    }



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

    /**
     * Pendiente de la recta de mínimos cuadrados: m = Σ(x-x̄)(y-ȳ) / Σ(x-x̄)².
     * x = número de ronda, y = posición final. Requiere al menos 3 puntos.
     */
    static BigDecimal pendienteDeRegresionLineal(List<double[]> puntos) {
        if (puntos.size() < 3) {
            return null;
        }
        double mediaX = puntos.stream().mapToDouble(punto -> punto[0]).average().orElse(0);
        double mediaY = puntos.stream().mapToDouble(punto -> punto[1]).average().orElse(0);
        double numerador = 0;
        double denominador = 0;
        for (double[] punto : puntos) {
            numerador += (punto[0] - mediaX) * (punto[1] - mediaY);
            denominador += (punto[0] - mediaX) * (punto[0] - mediaX);
        }
        return denominador == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(numerador / denominador).setScale(3, RoundingMode.HALF_UP);
    }

    /** Desviación estándar poblacional: mide la CONSISTENCIA (menor = más regular). */
    static BigDecimal desviacionEstandar(List<Integer> valores) {
        if (valores == null || valores.size() < 2) {
            return null;
        }
        double media = valores.stream().mapToInt(Integer::intValue).average().orElse(0);
        double varianza = valores.stream().mapToDouble(valor -> (valor - media) * (valor - media)).sum() / valores.size();
        return BigDecimal.valueOf(Math.sqrt(varianza)).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal puntosOCero(Participacion participacion) {
        return participacion != null && participacion.puntos() != null ? participacion.puntos() : BigDecimal.ZERO;
    }

    private static String claveEventoYPiloto(Long eventoId, Long pilotoId) {
        return eventoId + "-" + pilotoId;
    }

    private static Map<String, Participacion> indexarPorEventoYPiloto(List<Participacion> participaciones) {
        Map<String, Participacion> indice = new HashMap<>();
        participaciones.forEach(participacion -> indice.put(
                claveEventoYPiloto(participacion.eventoId(), participacion.pilotoId()), participacion));
        return indice;
    }


}

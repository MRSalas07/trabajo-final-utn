package com.tfi.motorsport.backend.clients.adapter;

import com.tfi.motorsport.backend.clients.clientHttpExternal.ClientHttpExternal;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.*;
import com.tfi.motorsport.backend.config.AppProperties;
import com.tfi.motorsport.backend.handlers.exceptions.IntegrationExternalException;
import com.tfi.motorsport.backend.model.Catalogo.*;
import com.tfi.motorsport.backend.clients.clientHttpExternal.FormatJson;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.EquipoNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.EventoNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.PilotoNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.ResultadoNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.SesionNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.TemporadaNorm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class F1Adapter implements FuenteDatosCategoria{

    static final String PROVEEDOR = "JOLPICA";
    private static final int TAMANIO_PAGINA = 100;
    /** Hora asumida cuando la API no informa la hora (temporadas viejas): mediodía UTC no cambia el día en América. */
    private static final String HORA_POR_DEFECTO = "12:00:00Z";

    private final ClientHttpExternal cliente;

    public F1Adapter(AppProperties props, JsonMapper jsonMapper) {
        AppProperties.Integracion cfg = props.integracion();
        this.cliente = new ClientHttpExternal(cfg.f1().urlBase(), jsonMapper,PROVEEDOR,
                cfg.timeoutConexionMs(), cfg.timeoutLecturaMs(), cfg.reintentos(), cfg.f1().intervaloMinimoMs());
    }

    @Override
    public String siglasCategoria() {
        return "F1";
    }

    @Override
    public String proveedor() {
        return PROVEEDOR;
    }

    @Override
    public TemporadaNorm obtenerTemporada(int anio, List<String> avisos) {
        // 1) Calendario
        JsonNode calendario = cliente.get(anio + ".json?limit=" + TAMANIO_PAGINA);
        List<JsonNode> carreras = FormatJson.lista(calendario, "MRData", "RaceTable", "Races");
        if (carreras.isEmpty()) {
            throw new IntegrationExternalException(PROVEEDOR, IntegrationExternalException.Tipo.RESPUESTA_VACIA,
                    "No hay calendario de F1 publicado para " + anio);
        }

        // 2) Resultados agrupados por número de fecha (round)
        Map<String, List<JsonNode>> carrera = descargarPaginado(anio + "/results.json", "Results");
        Map<String, List<JsonNode>> clasificacion = descargarPaginado(anio + "/qualifying.json", "QualifyingResults");
        Map<String, List<JsonNode>> sprint = descargarPaginado(anio + "/sprint.json", "SprintResults");

        Instant ahora = Instant.now();
        List<EventoNorm> eventos = new ArrayList<>();
        for (JsonNode race : carreras) {
            eventos.add(mapearEvento(race, carrera, clasificacion, sprint, ahora, avisos));
        }

        // 3) Campeón oficial, solo si todas las carreras del calendario ya tienen resultados
        boolean temporadaCompleta = carreras.stream()
                .allMatch(race -> carrera.containsKey(FormatJson.aString(race, "round")));
        String idExternoCampeon = temporadaCompleta ? campeonOficial(anio, avisos) : null;

        return new TemporadaNorm("F1", anio, "Fórmula 1 " + anio, PROVEEDOR, eventos, idExternoCampeon);
    }

    /**
     * Primero de la tabla oficial de pilotos. Hasta 1990 la F1 descartaba los peores resultados,
     * así que el campeón no siempre es el que más puntos sumó (ej: 1988, Senna sobre Prost).
     * Si falla, la sincronización sigue: el palmarés usará la tabla calculada.
     */
    private String campeonOficial(int anio, List<String> avisos) {
        try {
            JsonNode respuesta = cliente.get(anio + "/driverStandings.json?limit=1");
            for (JsonNode tabla : FormatJson.lista(respuesta, "MRData", "StandingsTable", "StandingsLists")) {
                for (JsonNode fila : FormatJson.lista(tabla, "DriverStandings")) {
                    String driverId = FormatJson.aString(fila, "Driver", "driverId");
                    if ("1".equals(FormatJson.aString(fila, "position")) && driverId != null) {
                        return "F1:" + driverId;
                    }
                }
            }
            avisos.add("F1 " + anio + ": la tabla oficial de pilotos no informa campeón");
        } catch (IntegrationExternalException ex) {
            avisos.add("F1 " + anio + ": no se pudo obtener el campeón oficial (" + ex.getMessage() + ")");
        }
        return null;
    }



    /**
     * Descarga todas las páginas de un endpoint de resultados y agrupa las filas por "round".
     * Una carrera puede quedar partida entre dos páginas, por eso se acumula en un mapa.
     */
    private Map<String, List<JsonNode>> descargarPaginado(String ruta, String campoResultados) {
        Map<String, List<JsonNode>> porRonda = new HashMap<>();
        int offset = 0;
        int total;
        do {
            JsonNode pagina = cliente.get(ruta + "?limit=" + TAMANIO_PAGINA + "&offset=" + offset);
            Integer t = FormatJson.aInt(pagina, "MRData", "total");
            total = t != null ? t : 0;
            for (JsonNode race : FormatJson.lista(pagina, "MRData", "RaceTable", "Races")) {
                String ronda = FormatJson.aString(race, "round");
                porRonda.computeIfAbsent(ronda, k -> new ArrayList<>())
                        .addAll(FormatJson.lista(race, campoResultados));
            }
            offset += TAMANIO_PAGINA;
        } while (offset < total);
        return porRonda;
    }



    private EventoNorm mapearEvento(JsonNode race, Map<String, List<JsonNode>> resCarrera,
                                    Map<String, List<JsonNode>> resClasif, Map<String, List<JsonNode>> resSprint,
                                    Instant ahora, List<String> avisos) {
        String ronda = FormatJson.aString(race, "round");
        String nombre = FormatJson.aString(race, "raceName");
        String circuitId = FormatJson.aString(race, "Circuit", "circuitId");
        String pais = FormatJson.aString(race, "Circuit", "Location", "country");

        // Sesiones del fin de semana (cada una con fecha + hora UTC)
        List<SesionNorm> sesiones = new ArrayList<>();
        agregarSesion(sesiones, race.get("FirstPractice"), "fp1", "Práctica 1", TipoSesion.PRACTICA, 1, 60, null, ahora);
        agregarSesion(sesiones, race.get("SecondPractice"), "fp2", "Práctica 2", TipoSesion.PRACTICA, 2, 60, null, ahora);
        agregarSesion(sesiones, race.get("ThirdPractice"), "fp3", "Práctica 3", TipoSesion.PRACTICA, 3, 60, null, ahora);
        JsonNode sq = race.has("SprintQualifying") ? race.get("SprintQualifying") : race.get("SprintShootout");
        agregarSesion(sesiones, sq, "sq", "Clasificación Sprint", TipoSesion.SPRINT_CLASIFICACION, 1, 45, null, ahora);
        agregarSesion(sesiones, race.get("Sprint"), "sprint", "Sprint", TipoSesion.SPRINT, 1, 60,
                mapearResultadosCarrera(resSprint.get(ronda)), ahora);
        List<ResultadoNorm> resultadosClasificacion = mapearResultadosClasificacion(resClasif.get(ronda));
        if (race.has("Qualifying")) {
            agregarSesion(sesiones, race.get("Qualifying"), "q", "Clasificación", TipoSesion.CLASIFICACION, 1, 60,
                    resultadosClasificacion, ahora);
        } else if (!resultadosClasificacion.isEmpty()) {
            // Temporadas viejas: hay resultados de clasificación pero no el horario de la sesión
            agregarSesion(sesiones, diaAnterior(FormatJson.aString(race, "date")), HORA_POR_DEFECTO, "q", "Clasificación",
                    TipoSesion.CLASIFICACION, 1, 60, resultadosClasificacion, ahora);
        }
        // La carrera usa la fecha/hora del nodo raíz
        agregarSesion(sesiones, race, "race", "Carrera", TipoSesion.CARRERA, 1, 120,
                mapearResultadosCarrera(resCarrera.get(ronda)), ahora);

        Instant inicio = sesiones.stream().map(SesionNorm::inicio).filter(i -> i != null)
                .min(Comparator.naturalOrder()).orElse(null);
        Instant fin = sesiones.stream().map(SesionNorm::fin).filter(i -> i != null)
                .max(Comparator.naturalOrder()).orElse(inicio);

        boolean tieneResultadosCarrera = resCarrera.containsKey(ronda) && !resCarrera.get(ronda).isEmpty();
        String estado = estadoPorFechas(inicio, fin, ahora, tieneResultadosCarrera);
        if (!tieneResultadosCarrera && fin != null && fin.isBefore(ahora)) {
            avisos.add("F1 fecha " + ronda + " (" + nombre + "): la carrera ya ocurrió pero aún no hay resultados publicados");
        }

        return new EventoNorm(
                ronda, nombre, nombre != null ? nombre.replace(" Grand Prix", " GP") : null,
                FormatJson.aInt(race, "round"), pais,
                FormatJson.aString(race, "Circuit", "Location", "locality"),
                FormatJson.aString(race, "Circuit", "circuitName"),
                ZonasHorariasF1.resolver(circuitId, pais),
                inicio, fin, estado, sesiones, List.of());
    }

    private void agregarSesion(List<SesionNorm> sesiones, JsonNode nodo, String id, String nombre, String tipo,
                               int numero, int duracionMin, List<ResultadoNorm> resultados, Instant ahora) {
        if (nodo == null) {
            return; // la sesión no existe en este fin de semana (ej: FP2 en formato sprint)
        }
        agregarSesion(sesiones, FormatJson.aString(nodo, "date"), FormatJson.aString(nodo, "time"), id, nombre, tipo,
                numero, duracionMin, resultados, ahora);
    }

    private void agregarSesion(List<SesionNorm> sesiones, String fecha, String hora, String id, String nombre,
                               String tipo, int numero, int duracionMin, List<ResultadoNorm> resultados, Instant ahora) {
        Instant inicio = fecha == null ? null : FormatJson.instantUtc(fecha + "T" + (hora != null ? hora : HORA_POR_DEFECTO));
        Instant fin = inicio == null ? null : inicio.plus(Duration.ofMinutes(duracionMin));
        boolean conResultados = resultados != null && !resultados.isEmpty();
        sesiones.add(new SesionNorm(id, nombre, tipo, numero, inicio, fin, null, null,
                estadoPorFechas(inicio, fin, ahora, conResultados),
                resultados != null ? resultados : List.of()));
    }

    /** Las temporadas viejas no informan cuándo fue la clasificación: se asume el día anterior a la carrera. */
    private static String diaAnterior(String fecha) {
        return fecha == null ? null : LocalDate.parse(fecha).minusDays(1).toString();
    }

    /** Resultados de carrera o sprint. */
    List<ResultadoNorm> mapearResultadosCarrera(List<JsonNode> filas) {
        if (filas == null) {
            return List.of();
        }
        // El tiempo del ganador permite calcular la diferencia de cada piloto en segundos
        Long millisGanador = filas.stream()
                .filter(f -> "1".equals(FormatJson.aString(f, "position")))
                .map(f -> FormatJson.aLong(f, "Time", "millis"))
                .findFirst().orElse(null);

        List<ResultadoNorm> lista = new ArrayList<>();
        for (JsonNode f : filas) {
            Long millis = FormatJson.aLong(f, "Time", "millis");
            String status = FormatJson.aString(f, "status");
            String estado = estadoResultado(FormatJson.aString(f, "positionText"), status);
            lista.add(new ResultadoNorm(
                    piloto(f.get("Driver")), equipo(f.get("Constructor")),
                    FormatJson.aInt(f, "position"), FormatJson.aInt(f, "number"),
                    FormatJson.aDecimal(f, "points"),
                    millis != null ? segundos(millis) : null,
                    diferencia(millis, millisGanador),
                    FormatJson.aInt(f, "laps"),
                    tiempoVuelta(FormatJson.aString(f, "FastestLap", "Time", "time")),
                    estado,
                    EstadoResultado.FINALIZADO.equals(estado) ? null : status,
                    null)); // en F1 la marca es la del equipo
        }
        return lista;
    }

    /** Resultados de clasificación: el tiempo es el mejor entre Q3, Q2 y Q1. */
    List<ResultadoNorm> mapearResultadosClasificacion(List<JsonNode> filas) {
        if (filas == null) {
            return List.of();
        }
        List<ResultadoNorm> lista = new ArrayList<>();
        BigDecimal pole = null;
        for (JsonNode f : filas) {
            BigDecimal mejor = null;
            for (String q : new String[]{"Q3", "Q2", "Q1"}) {
                BigDecimal t = tiempoVuelta(FormatJson.aString(f, q));
                if (t != null) {
                    mejor = t;
                    break;
                }
            }
            Integer pos = FormatJson.aInt(f, "position");
            if (pos != null && pos == 1) {
                pole = mejor;
            }
            lista.add(new ResultadoNorm(
                    piloto(f.get("Driver")), equipo(f.get("Constructor")),
                    pos, FormatJson.aInt(f, "number"), BigDecimal.ZERO,
                    mejor, null, null, mejor,
                    mejor != null ? EstadoResultado.FINALIZADO : EstadoResultado.NO_LARGO, null, null));
        }
        // Diferencia con la pole (segundo recorrido porque la pole puede no venir primera)
        final BigDecimal tiempoPole = pole;
        return lista.stream().map(r -> tiempoPole == null || r.tiempo() == null ? r
                : new ResultadoNorm(r.piloto(), r.equipo(), r.posicion(), r.numero(), r.puntos(), r.tiempo(),
                r.tiempo().subtract(tiempoPole).max(BigDecimal.ZERO), r.vueltas(), r.mejorVuelta(),
                r.estado(), r.motivoAbandono(), r.marca())).toList();
    }

    private PilotoNorm piloto(JsonNode d) {
        return new PilotoNorm(
                "F1:" + FormatJson.aString(d, "driverId"),
                FormatJson.aString(d, "givenName"), FormatJson.aString(d, "familyName"),
                FormatJson.aDate(d, "dateOfBirth"), FormatJson.aString(d, "nationality"), null,
                FormatJson.aString(d, "code"), FormatJson.aInt(d, "permanentNumber"));
    }

    private EquipoNorm equipo(JsonNode c) {
        String nombre = FormatJson.aString(c, "name");
        return new EquipoNorm("F1:" + FormatJson.aString(c, "constructorId"), nombre, nombre, FormatJson.aString(c, "nationality"));
    }



    /**
     * Traduce el estado de Jolpica al catálogo propio.
     * positionText: número = clasificado, R = retirado, D = descalificado, E = excluido,
     * W = retirado antes de largar, F = no clasificó, N = no clasificado.
     */
    static String estadoResultado(String positionText, String status) {
        if (positionText == null) {
            return EstadoResultado.FINALIZADO;
        }
        return switch (positionText) {
            case "R", "N" -> EstadoResultado.ABANDONO;
            case "D", "E" -> EstadoResultado.DESCALIFICADO;
            case "W", "F" -> EstadoResultado.NO_LARGO;
            default -> (status != null && (status.equals("Finished") || status.startsWith("+")
                    || status.equals("Lapped") || status.contains("Lap")))
                    ? EstadoResultado.FINALIZADO : EstadoResultado.ABANDONO;
        };
    }

    static String estadoPorFechas(Instant inicio, Instant fin, Instant ahora, boolean conResultados) {
        if (conResultados) {
            return EstadoEvento.FINALIZADO;
        }
        if (inicio == null || ahora.isBefore(inicio)) {
            return EstadoEvento.PROGRAMADO;
        }
        if (fin != null && ahora.isAfter(fin)) {
            return EstadoEvento.FINALIZADO;
        }
        return EstadoEvento.EN_CURSO;
    }

    /**
     * Diferencia con el ganador. Puede dar negativa cuando un piloto recibió una penalización
     * de tiempo posterior a la carrera (su tiempo bruto fue menor): en ese caso no se informa.
     */
    private static BigDecimal diferencia(Long millis, Long millisGanador) {
        if (millis == null || millisGanador == null || millis < millisGanador) {
            return null;
        }
        return segundos(millis - millisGanador);
    }

    private static BigDecimal segundos(long millis) {
        return BigDecimal.valueOf(millis).divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
    }

    /** Convierte "1:22.167" o "58.123" a segundos (82.167). */
    static BigDecimal tiempoVuelta(String aString) {
        if (aString == null || aString.isBlank()) {
            return null;
        }
        try {
            String[] partes = aString.trim().split(":");
            BigDecimal total = BigDecimal.ZERO;
            for (String parte : partes) {
                total = total.multiply(BigDecimal.valueOf(60)).add(new BigDecimal(parte));
            }
            return total.setScale(3, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

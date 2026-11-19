package com.tfi.motorsport.backend.clients.adapter;

import com.tfi.motorsport.backend.config.AppProperties;
import com.tfi.motorsport.backend.handlers.exceptions.IntegrationExternalException;
import com.tfi.motorsport.backend.clients.adapter.FuenteDatosCategoria;
import com.tfi.motorsport.backend.clients.clientHttpExternal.ClientHttpExternal;
import com.tfi.motorsport.backend.clients.clientHttpExternal.FormatJson;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.EquipoNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.EtapaNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.EventoNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.PilotoNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.ResultadoEtapaNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.TemporadaNorm;
import com.tfi.motorsport.backend.model.Catalogo;
import com.tfi.motorsport.backend.model.Catalogo.EstadoEvento;
import com.tfi.motorsport.backend.model.Catalogo.EstadoResultado;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Adaptador de WRC sobre la API NO OFICIAL que usa el sitio wrc.com
 * (alojada por Red Bull Media House: p-p.redbull.com/rb-wrccom-lintegration-yv-prod/api/).
 *
 * ADVERTENCIA: no está documentada ni garantizada. Puede cambiar o dejar de funcionar
 * sin aviso (el dominio anterior, api.wrc.com, ya fue dado de baja). Por eso:
 *  - se lee el JSON de forma tolerante (campos faltantes = null);
 *  - el adaptador puede deshabilitarse con WRC_HABILITADO=false;
 *  - cada evento se procesa por separado: si uno falla, se registra un aviso y se sigue.
 *
 * Observación verificada: las fechas sin zona (ej: "firstCarDueDateTime") están en UTC
 * (los registros que traen ambas versiones muestran 18:41 UTC = 19:41+01:00 local).
 *
 * Endpoints usados (relativos a la URL base):
 *  seasons.json, season-detail.json?seasonId=, championship-detail.json, championship-overall-results.json,
 *  events/{id}.json, events/{id}/stages.json, events/{id}/rallies/{rallyId}/entries.json,
 *  events/{id}/stages/{stageId}/stagetimes.json, events/{id}/stages/{stageId}/results.json,
 *  events/{id}/retirements.json
 *
 * Para limitar el volumen de datos y de consultas, se guardan solo las tripulaciones
 * de la clase principal (Rally1) y cualquier otra que haya sumado puntos en el rally.
 */
@Slf4j
@Component
public class WrcAdapter implements FuenteDatosCategoria {

    static final String PROVEEDOR = "WRC_API_NO_OFICIAL";
    private static final String NOMBRE_TEMPORADA = "World Rally Championship";
    private static final String GRUPO_PRINCIPAL = "Rally1";
    /** Se descarga el itinerario de rallies que empiezan dentro de esta cantidad de días. */
    private static final int DIAS_ANTICIPACION_ITINERARIO = 14;

    private final ClientHttpExternal cliente;
    private final boolean habilitado;

    public WrcAdapter(AppProperties props, JsonMapper jsonMapper) {
        AppProperties.Integracion cfg = props.integracion();
        this.habilitado = cfg.wrc().habilitado();
        this.cliente = new ClientHttpExternal( cfg.wrc().urlBase(), jsonMapper,PROVEEDOR,
                cfg.timeoutConexionMs(), cfg.timeoutLecturaMs(), cfg.reintentos(), cfg.wrc().intervaloMinimoMs());
    }

    @Override
    public String siglasCategoria() {
        return "WRC";
    }

    @Override
    public String proveedor() {
        return PROVEEDOR;
    }

    @Override
    public boolean habilitada() {
        return habilitado;
    }

    @Override
    public TemporadaNorm obtenerTemporada(int anio, List<String> avisos) {
        int seasonId = buscarSeasonId(anio);
        JsonNode detalle = cliente.get("season-detail.json?seasonId=" + seasonId);

        // Puntos del campeonato de pilotos por evento: eventId -> (personId -> puntos)
        Map<Long, Map<Long, BigDecimal>> puntos = obtenerPuntos(detalle, seasonId, avisos);

        List<JsonNode> rondas = new ArrayList<>(FormatJson.lista(detalle, "seasonRounds"));
        rondas.sort(Comparator.comparing(r -> {
            Integer o = FormatJson.aInt(r, "order");
            return o != null ? o : Integer.MAX_VALUE;
        }));

        List<EventoNorm> eventos = new ArrayList<>();
        for (JsonNode ronda : rondas) {
            JsonNode ev = ronda.get("event");
            if (ev == null) {
                continue;
            }
            try {
                eventos.add(mapearEvento(ev, FormatJson.aInt(ronda, "order"), puntos, avisos));
            } catch (IntegrationExternalException ex) {
                // Un rally con datos inaccesibles no debe impedir sincronizar el resto
                avisos.add("WRC: no se pudo procesar '" + FormatJson.aString(ev, "name") + "': " + ex.getMessage());
                log.warn("WRC evento {} omitido: {}", FormatJson.aString(ev, "eventId"), ex.getMessage());
            }
        }
        return new TemporadaNorm("WRC", anio, "WRC " + anio, PROVEEDOR, eventos);
    }

    // ------------------------------------------------------------------ temporada y puntos

    private int buscarSeasonId(int anio) {
        for (JsonNode s : FormatJson.lista(cliente.get("seasons.json"))) {
            Integer year = FormatJson.aInt(s, "year");
            if (year != null && year == anio && NOMBRE_TEMPORADA.equalsIgnoreCase(FormatJson.aString(s, "name"))) {
                return FormatJson.aInt(s, "seasonId");
            }
        }
        throw new IntegrationExternalException(PROVEEDOR, IntegrationExternalException.Tipo.RESPUESTA_VACIA,
                "No existe temporada de WRC " + anio + " en el proveedor");
    }

    private Map<Long, Map<Long, BigDecimal>> obtenerPuntos(JsonNode detalle, int seasonId, List<String> avisos) {
        Map<Long, Map<Long, BigDecimal>> resultado = new HashMap<>();
        Long championshipId = FormatJson.lista(detalle, "championships").stream()
                .filter(c -> "Drivers".equalsIgnoreCase(FormatJson.aString(c, "type")))
                .filter(c -> {
                    String n = FormatJson.aString(c, "name");
                    return n != null && n.startsWith("FIA World Rally Championship for Drivers");
                })
                .map(c -> FormatJson.aLong(c, "championshipId"))
                .findFirst().orElse(null);
        if (championshipId == null) {
            avisos.add("WRC: no se encontró el campeonato de pilotos; los puntos quedarán vacíos");
            return resultado;
        }
        try {
            String q = "?championshipId=" + championshipId + "&seasonId=" + seasonId;
            // championshipEntryId -> personId
            Map<Long, Long> entradaAPersona = new HashMap<>();
            for (JsonNode e : FormatJson.lista(cliente.get("championship-detail.json" + q), "championshipEntries")) {
                entradaAPersona.put(FormatJson.aLong(e, "championshipEntryId"), FormatJson.aLong(e, "personId"));
            }
            for (JsonNode er : FormatJson.lista(cliente.get("championship-overall-results.json" + q), "entryResults")) {
                Long personId = entradaAPersona.get(FormatJson.aLong(er, "championshipEntryId"));
                for (JsonNode rr : FormatJson.lista(er, "roundResults")) {
                    BigDecimal pts = FormatJson.aDecimal(rr, "totalPoints");
                    Long eventId = FormatJson.aLong(rr, "eventId");
                    if (personId != null && eventId != null && pts != null) {
                        resultado.computeIfAbsent(eventId, k -> new HashMap<>()).put(personId, pts);
                    }
                }
            }
        } catch (IntegrationExternalException ex) {
            avisos.add("WRC: no se pudieron obtener los puntos del campeonato: " + ex.getMessage());
        }
        return resultado;
    }

    // ------------------------------------------------------------------ evento

    private EventoNorm mapearEvento(JsonNode ev, Integer orden, Map<Long, Map<Long, BigDecimal>> puntos,
                                    List<String> avisos) {
        long eventId = FormatJson.aLong(ev, "eventId");
        String nombre = FormatJson.aString(ev, "name");
        ZoneId zona = zona(FormatJson.aString(ev, "timeZoneId"));
        LocalDate desde = FormatJson.aDate(ev, "startDate");
        LocalDate hasta = FormatJson.aDate(ev, "finishDate");
        Instant inicio = desde != null ? desde.atStartOfDay(zona).toInstant() : null;
        Instant fin = hasta != null ? hasta.plusDays(1).atStartOfDay(zona).toInstant().minusSeconds(1) : inicio;
        Instant ahora = Instant.now();

        String ubicacion = FormatJson.aString(ev, "location");
        String ciudad = ubicacion != null && ubicacion.contains(",") ? ubicacion.substring(0, ubicacion.indexOf(',')).trim() : ubicacion;

        List<EtapaNorm> etapas = new ArrayList<>();
        boolean descargarItinerario = inicio != null
                && inicio.isBefore(ahora.plus(DIAS_ANTICIPACION_ITINERARIO, ChronoUnit.DAYS));
        if (descargarItinerario) {
            etapas = mapearEtapas(eventId, desde, zona, puntos.getOrDefault(eventId, Map.of()), avisos, nombre);
        }

        String estado;
        if (ahora.isBefore(inicio != null ? inicio : ahora.plusSeconds(1))) {
            estado = EstadoEvento.PROGRAMADO;
        } else if (fin != null && ahora.isAfter(fin)) {
            estado = EstadoEvento.FINALIZADO;
        } else {
            estado = EstadoEvento.EN_CURSO;
        }

        return new EventoNorm(String.valueOf(eventId), nombre, nombreCorto(nombre), orden,
                FormatJson.aString(ev, "country", "name"), ciudad, ubicacion, zona.getId(),
                inicio, fin, estado, List.of(), etapas);
    }

    private List<EtapaNorm> mapearEtapas(long eventId, LocalDate inicioEvento, ZoneId zona,
                                         Map<Long, BigDecimal> puntosEvento, List<String> avisos, String nombreEvento) {
        // Rally principal del evento
        Long rallyId = FormatJson.lista(cliente.get("events/" + eventId + ".json"), "rallies").stream()
                .filter(r -> FormatJson.booleano(r, "isMain"))
                .map(r -> FormatJson.aLong(r, "rallyId"))
                .findFirst().orElse(null);
        if (rallyId == null) {
            avisos.add("WRC '" + nombreEvento + "': sin rally principal definido");
            return List.of();
        }

        // Inscriptos: entryId -> datos de la tripulación. Solo Rally1 o con puntos.
        Map<Long, Tripulante> tripulaciones = new HashMap<>();
        for (JsonNode e : FormatJson.lista(cliente.get("events/" + eventId + "/rallies/" + rallyId + "/entries.json"))) {
            Tripulante t = mapearTripulante(e);
            if (t == null) {
                continue;
            }
            boolean principal = GRUPO_PRINCIPAL.equalsIgnoreCase(FormatJson.aString(e, "group", "name"));
            BigDecimal pts = puntosEvento.get(t.personIdPiloto);
            if (principal || (pts != null && pts.signum() > 0)) {
                tripulaciones.put(t.entryId, t.conPuntos(pts));
            }
        }

        List<JsonNode> tramos = FormatJson.lista(cliente.get("events/" + eventId + "/stages.json"));
        tramos.sort(Comparator.comparing(s -> {
            Integer n = FormatJson.aInt(s, "number");
            return n != null ? n : Integer.MAX_VALUE;
        }));

        List<EtapaNorm> etapas = new ArrayList<>();
        JsonNode ultimoTramoCompleto = null;
        for (JsonNode s : tramos) {
            String estado = estadoTramo(FormatJson.aString(s, "status"));
            Instant inicioTramo = inicioDeTramo(s);
            Integer dia = (inicioTramo != null && inicioEvento != null)
                    ? (int) ChronoUnit.DAYS.between(inicioEvento, inicioTramo.atZone(zona).toLocalDate()) + 1
                    : null;
            List<ResultadoEtapaNorm> resultados = List.of();
            if (EstadoEvento.FINALIZADO.equals(estado) && !tripulaciones.isEmpty()) {
                resultados = tiemposDeTramo(eventId, rallyId, FormatJson.aLong(s, "stageId"), tripulaciones);
                ultimoTramoCompleto = s;
            }
            String code = FormatJson.aString(s, "code");
            String nombre = (code != null ? code + " " : "") + FormatJson.aString(s, "name");
            etapas.add(new EtapaNorm(FormatJson.aString(s, "stageId"), FormatJson.aInt(s, "number"), nombre, dia,
                    FormatJson.aDecimal(s, "distance"), inicioTramo, null, estado, resultados));
        }

        // Clasificación general = resultados acumulados luego del último tramo completado
        if (ultimoTramoCompleto != null) {
            etapas.add(clasificacionGeneral(eventId, rallyId, ultimoTramoCompleto, tramos, tripulaciones, avisos, nombreEvento));
        }
        return etapas;
    }

    private List<ResultadoEtapaNorm> tiemposDeTramo(long eventId, long rallyId, Long stageId,
                                                    Map<Long, Tripulante> tripulaciones) {
        List<ResultadoEtapaNorm> lista = new ArrayList<>();
        JsonNode tiempos = cliente.get("events/" + eventId + "/stages/" + stageId + "/stagetimes.json?rallyId=" + rallyId);
        for (JsonNode t : FormatJson.lista(tiempos)) {
            Tripulante tr = tripulaciones.get(FormatJson.aLong(t, "entryId"));
            if (tr == null) {
                continue;
            }
            boolean completo = "Completed".equalsIgnoreCase(FormatJson.aString(t, "status"));
            lista.add(new ResultadoEtapaNorm(tr.piloto, tr.copiloto, tr.equipo, tr.numero,
                    completo ? FormatJson.aInt(t, "position") : null,
                    msASegundos(FormatJson.aLong(t, "elapsedDurationMs")),
                    msASegundos(FormatJson.aLong(t, "diffFirstMs")),
                    null,
                    completo ? EstadoResultado.FINALIZADO : EstadoResultado.ABANDONO,
                    completo ? null : FormatJson.aString(t, "status")));
        }
        return lista;
    }

    private EtapaNorm clasificacionGeneral(long eventId, long rallyId, JsonNode ultimoTramo, List<JsonNode> tramos,
                                           Map<Long, Tripulante> tripulaciones, List<String> avisos, String nombreEvento) {
        Long stageId = FormatJson.aLong(ultimoTramo, "stageId");
        List<ResultadoEtapaNorm> resultados = new ArrayList<>();
        Set<Long> clasificados = new HashSet<>();
        for (JsonNode r : FormatJson.lista(cliente.get("events/" + eventId + "/stages/" + stageId + "/results.json?rallyId=" + rallyId))) {
            Tripulante tr = tripulaciones.get(FormatJson.aLong(r, "entryId"));
            if (tr == null) {
                continue;
            }
            clasificados.add(tr.entryId);
            resultados.add(new ResultadoEtapaNorm(tr.piloto, tr.copiloto, tr.equipo, tr.numero,
                    FormatJson.aInt(r, "position"),
                    msASegundos(FormatJson.aLong(r, "totalTimeMs")),
                    msASegundos(FormatJson.aLong(r, "diffFirstMs")),
                    tr.puntos != null ? tr.puntos : BigDecimal.ZERO,
                    EstadoResultado.FINALIZADO, null));
        }

        // Tripulaciones que no figuran en la general: abandono (con el motivo si se conoce)
        Map<Long, String> motivos = new HashMap<>();
        try {
            for (JsonNode ret : FormatJson.lista(cliente.get("events/" + eventId + "/retirements.json"))) {
                motivos.put(FormatJson.aLong(ret, "entryId"), FormatJson.aString(ret, "reason"));
            }
        } catch (IntegrationExternalException ex) {
            avisos.add("WRC '" + nombreEvento + "': no se pudieron obtener los abandonos");
        }
        for (Tripulante tr : tripulaciones.values()) {
            if (!clasificados.contains(tr.entryId)) {
                resultados.add(new ResultadoEtapaNorm(tr.piloto, tr.copiloto, tr.equipo, tr.numero, null, null, null,
                        tr.puntos != null ? tr.puntos : BigDecimal.ZERO,
                        EstadoResultado.ABANDONO, motivos.getOrDefault(tr.entryId, "Abandono")));
            }
        }

        boolean rallyTerminado = tramos.stream().allMatch(s -> !EstadoEvento.PROGRAMADO.equals(estadoTramo(FormatJson.aString(s, "status"))));
        Integer ultimoNumero = FormatJson.aInt(ultimoTramo, "number");
        return new EtapaNorm(Catalogo.ETAPA_GENERAL, ultimoNumero != null ? ultimoNumero + 1 : 999,
                "Clasificación general", null, null, inicioDeTramo(ultimoTramo), null,
                rallyTerminado ? EstadoEvento.FINALIZADO : EstadoEvento.EN_CURSO, resultados);
    }

    // ------------------------------------------------------------------ tripulaciones

    /** Datos de una inscripción (piloto + copiloto + equipo) ya normalizados. */
    private record Tripulante(Long entryId, Long personIdPiloto, PilotoNorm piloto, PilotoNorm copiloto,
                              EquipoNorm equipo, Integer numero, BigDecimal puntos) {
        Tripulante conPuntos(BigDecimal p) {
            return new Tripulante(entryId, personIdPiloto, piloto, copiloto, equipo, numero, p);
        }
    }

    private Tripulante mapearTripulante(JsonNode e) {
        JsonNode d = e.get("driver");
        JsonNode c = e.get("codriver");
        if (d == null || c == null) {
            return null; // sin copiloto no se puede formar una tripulación válida
        }
        EquipoNorm equipo;
        JsonNode entrant = e.get("entrant");
        String fabricante = FormatJson.aString(e, "manufacturer", "name");
        if (entrant != null && FormatJson.aLong(entrant, "entrantId") != null) {
            equipo = new EquipoNorm("WRC:E" + FormatJson.aLong(entrant, "entrantId"),
                    nombreEquipo(FormatJson.aString(entrant, "name")), fabricante, null);
        } else if (fabricante != null) {
            equipo = new EquipoNorm("WRC:M" + FormatJson.aLong(e, "manufacturer", "manufacturerId"), fabricante, fabricante, null);
        } else {
            return null;
        }
        Integer numero = FormatJson.aInt(e, "identifier");
        return new Tripulante(FormatJson.aLong(e, "entryId"), FormatJson.aLong(d, "personId"),
                persona(d, numero), persona(c, null), equipo, numero, null);
    }

    /**
     * IMPORTANTE: el "personId" de la API de WRC NO es estable entre temporadas (la misma persona
     * tiene un id distinto en 2025 y en 2026). Para no duplicar pilotos y poder comparar temporadas,
     * el id externo se arma con el nombre normalizado: "WRC:thierry-neuville".
     */
    private PilotoNorm persona(JsonNode p, Integer numero) {
        return new PilotoNorm(idPersona(FormatJson.aString(p, "firstName"), FormatJson.aString(p, "lastName")),
                capitalizar(FormatJson.aString(p, "firstName")), capitalizar(FormatJson.aString(p, "lastName")),
                null, FormatJson.aString(p, "country", "iso3"), FormatJson.aString(p, "country", "name"),
                FormatJson.aString(p, "code"), numero);
    }

    // ------------------------------------------------------------------ utilidades

    private static Instant inicioDeTramo(JsonNode tramo) {
        for (JsonNode c : FormatJson.lista(tramo, "controls")) {
            if ("StageStart".equalsIgnoreCase(FormatJson.aString(c, "type"))) {
                return FormatJson.instantUtc(FormatJson.aString(c, "firstCarDueDateTime"));
            }
        }
        return null;
    }

    static String estadoTramo(String status) {
        if (status == null) {
            return EstadoEvento.PROGRAMADO;
        }
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "completed" -> EstadoEvento.FINALIZADO;
            case "cancelled", "canceled" -> EstadoEvento.CANCELADO;
            case "running", "interrupted", "active", "started" -> EstadoEvento.EN_CURSO;
            default -> EstadoEvento.PROGRAMADO;
        };
    }

    private static ZoneId zona(String id) {
        try {
            return id != null ? ZoneId.of(id) : ZoneId.of("UTC");
        } catch (DateTimeException e) {
            return ZoneId.of("UTC");
        }
    }

    private static BigDecimal msASegundos(Long ms) {
        return ms == null ? null : BigDecimal.valueOf(ms).divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
    }

    /** "Thierry", "NEUVILLE" -> "WRC:thierry-neuville" (sin tildes, minúsculas, separado por guiones). */
    static String idPersona(String nombre, String apellido) {
        String completo = ((nombre != null ? nombre : "") + " " + (apellido != null ? apellido : "")).trim();
        String sinTildes = java.text.Normalizer.normalize(completo, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return "WRC:" + sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    /** "Rallye Monte-Carlo" -> "Monte-Carlo"; "Rally Italia Sardegna" -> "Italia Sardegna". */
    static String nombreCorto(String nombre) {
        if (nombre == null) {
            return null;
        }
        String corto = nombre.replaceFirst("(?i)^(rallye|rally)\\s+", "").replaceFirst("(?i)\\s+(rallye|rally)$", "");
        return corto.length() > 60 ? corto.substring(0, 60) : corto;
    }

    /**
     * Nombres de equipo: "TOYOTA GAZOO RACING WRT" -> "Toyota Gazoo Racing WRT".
     * Las palabras cortas (siglas como WRT, M-SPORT) se mantienen en mayúsculas.
     */
    static String nombreEquipo(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        StringBuilder sb = new StringBuilder();
        for (String palabra : texto.trim().split("\s+")) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(palabra.length() <= 3 ? palabra.toUpperCase(Locale.ROOT) : capitalizar(palabra));
        }
        return sb.toString();
    }

    /** La API entrega apellidos en mayúsculas ("NEUVILLE"): se normalizan a "Neuville". */
    static String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        StringBuilder sb = new StringBuilder(texto.length());
        boolean inicioPalabra = true;
        for (char ch : texto.toLowerCase(Locale.ROOT).toCharArray()) {
            sb.append(inicioPalabra ? Character.toUpperCase(ch) : ch);
            inicioPalabra = ch == ' ' || ch == '-' || ch == '\'';
        }
        return sb.toString();
    }
}

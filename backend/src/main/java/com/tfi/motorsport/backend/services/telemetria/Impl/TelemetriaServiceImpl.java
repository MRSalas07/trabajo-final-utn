package com.tfi.motorsport.backend.services.telemetria.Impl;

import com.tfi.motorsport.backend.clients.clientHttpExternal.FormatJson;
import com.tfi.motorsport.backend.clients.openf1.OpenF1Cliente;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.PilotoTelemetria;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.SeriePosiciones;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.SerieVueltas;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.SesionTelemetria;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.TelemetriaSesion;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.services.telemetria.CacheTemporal;
import com.tfi.motorsport.backend.services.telemetria.ITelemetriaService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Análisis de sesiones de F1 con datos de OpenF1 (MVP 7, módulo plus).
 *
 * Diseño desacoplado: solo se crea si app.telemetria.habilitada=true, no escribe en la base
 * (consulta, transforma y guarda un rato en memoria) y no usa el resto de los servicios.
 *
 * OpenF1 da gratis los datos históricos desde 2023. Durante una sesión en curso sus datos requieren
 * suscripción paga, por eso el análisis es de sesiones terminadas.
 */
@Service
@ConditionalOnProperty(prefix = "app.telemetria", name = "habilitada", havingValue = "true")
public class TelemetriaServiceImpl implements ITelemetriaService {

    /** OpenF1 tiene datos desde la temporada 2023. */
    public static final int PRIMER_ANIO = 2023;
    /** Pilotos por gráfico de vueltas: con más líneas se vuelve ilegible. */
    public static final int MAX_PILOTOS_VUELTAS = 6;
    /** Si OpenF1 no informa el fin de una sesión, se la da por terminada 4 horas después de empezar. */
    private static final Duration DURACION_MAXIMA_SESION = Duration.ofHours(4);

    private final OpenF1Cliente cliente;
    private final CacheTemporal cache = new CacheTemporal(Duration.ofMinutes(30), 300);

    public TelemetriaServiceImpl(OpenF1Cliente cliente) {
        this.cliente = cliente;
    }

    @Override
    public List<SesionTelemetria> sesiones(int anio) {
        int anioActual = Year.now(ZoneOffset.UTC).getValue();
        if (anio < PRIMER_ANIO || anio > anioActual) {
            throw new RuleBusinessException("OpenF1 tiene datos desde " + PRIMER_ANIO + " hasta " + anioActual);
        }
        return cache.obtener("sesiones-" + anio, () -> {
            Instant ahora = Instant.now();
            return FormatJson.lista(cliente.get("sessions?year=" + anio)).stream()
                    .map(TelemetriaServiceImpl::aSesion)
                    .filter(s -> s.sessionKey() != null && s.inicio() != null && s.inicio().isBefore(ahora))
                    .sorted(Comparator.comparing(SesionTelemetria::inicio).reversed())
                    .toList();
        });
    }

    @Override
    public TelemetriaSesion sesion(int sessionKey, List<Integer> pilotos) {
        SesionTelemetria sesion = cache.obtener("sesion-" + sessionKey, () ->
                FormatJson.lista(cliente.get("sessions?session_key=" + sessionKey)).stream()
                        .findFirst()
                        .map(TelemetriaServiceImpl::aSesion)
                        .orElseThrow(() -> new ResourceNotFoundException("No existe la sesión " + sessionKey + " en OpenF1")));
        if (!terminada(sesion)) {
            return new TelemetriaSesion(sesion, List.of(), List.of(), List.of(),
                    "La sesión todavía no terminó: el análisis se habilita al finalizar "
                            + "(durante la sesión, los datos de OpenF1 requieren suscripción paga).");
        }

        Map<Integer, PilotoTelemetria> porNumero = new LinkedHashMap<>();
        for (PilotoTelemetria piloto : cache.obtener("pilotos-" + sessionKey, () -> pilotosDeSesion(sessionKey))) {
            porNumero.put(piloto.numero(), piloto);
        }
        List<SeriePosiciones> posiciones = cache.obtener("posiciones-" + sessionKey,
                () -> posiciones(sessionKey, sesion.inicio(), porNumero));
        Map<Integer, SerieVueltas> vueltas = cache.obtener("vueltas-" + sessionKey,
                () -> vueltas(sessionKey, porNumero));

        List<Integer> seleccion = (pilotos == null || pilotos.isEmpty())
                ? mejorUbicados(posiciones, porNumero)
                : pilotos.stream().filter(Objects::nonNull).distinct().limit(MAX_PILOTOS_VUELTAS).toList();
        List<SerieVueltas> series = seleccion.stream().map(vueltas::get).filter(Objects::nonNull).toList();
        return new TelemetriaSesion(sesion, List.copyOf(porNumero.values()), series, posiciones, null);
    }

    private List<PilotoTelemetria> pilotosDeSesion(int sessionKey) {
        List<PilotoTelemetria> lista = new ArrayList<>();
        Set<Integer> vistos = new HashSet<>();
        for (JsonNode d : FormatJson.lista(cliente.get("drivers?session_key=" + sessionKey))) {
            Integer numero = FormatJson.aInt(d, "driver_number");
            if (numero != null && vistos.add(numero)) {
                lista.add(new PilotoTelemetria(numero, FormatJson.aString(d, "full_name"),
                        FormatJson.aString(d, "name_acronym"), FormatJson.aString(d, "team_name"),
                        color(FormatJson.aString(d, "team_colour"))));
            }
        }
        return List.copyOf(lista);
    }

    /** OpenF1 informa cada CAMBIO de posición con su hora: se arma una serie por piloto, en orden. */
    private List<SeriePosiciones> posiciones(int sessionKey, Instant inicio, Map<Integer, PilotoTelemetria> pilotos) {
        record Cambio(Integer numero, Instant fecha, Integer posicion) {
        }
        List<Cambio> cambios = new ArrayList<>();
        for (JsonNode p : FormatJson.lista(cliente.get("position?session_key=" + sessionKey))) {
            Cambio cambio = new Cambio(FormatJson.aInt(p, "driver_number"),
                    FormatJson.instantUtc(FormatJson.aString(p, "date")), FormatJson.aInt(p, "position"));
            if (cambio.numero() != null && cambio.fecha() != null && cambio.posicion() != null) {
                cambios.add(cambio);
            }
        }
        cambios.sort(Comparator.comparing(Cambio::fecha));

        Map<Integer, SeriePosiciones> series = new LinkedHashMap<>();
        for (Cambio cambio : cambios) {
            SeriePosiciones serie = series.computeIfAbsent(cambio.numero(),
                    numero -> new SeriePosiciones(numero, acronimo(numero, pilotos.get(numero)),
                            colorDe(pilotos.get(numero)), new ArrayList<>(), new ArrayList<>()));
            long segundos = inicio == null ? 0 : Math.max(0, Duration.between(inicio, cambio.fecha()).getSeconds());
            serie.minutos().add(BigDecimal.valueOf(segundos).divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP));
            serie.posiciones().add(cambio.posicion());
        }
        return List.copyOf(series.values());
    }

    /**
     * Las vueltas de TODOS los pilotos en una sola consulta (cuida el cupo de OpenF1): después se elige
     * qué pilotos mostrar sin volver a consultar.
     */
    private Map<Integer, SerieVueltas> vueltas(int sessionKey, Map<Integer, PilotoTelemetria> pilotos) {
        Map<Integer, TreeMap<Integer, BigDecimal>> porPiloto = new LinkedHashMap<>();
        for (JsonNode v : FormatJson.lista(cliente.get("laps?session_key=" + sessionKey))) {
            Integer numero = FormatJson.aInt(v, "driver_number");
            Integer vuelta = FormatJson.aInt(v, "lap_number");
            BigDecimal tiempo = FormatJson.aDecimal(v, "lap_duration");
            // Las vueltas sin tiempo o de salida de boxes distorsionan el gráfico: se omiten
            if (numero != null && vuelta != null && tiempo != null && !FormatJson.booleano(v, "is_pit_out_lap")) {
                porPiloto.computeIfAbsent(numero, n -> new TreeMap<>()).put(vuelta, tiempo.setScale(3, RoundingMode.HALF_UP));
            }
        }
        Map<Integer, SerieVueltas> series = new LinkedHashMap<>();
        porPiloto.forEach((numero, tiempos) -> series.put(numero, new SerieVueltas(numero,
                acronimo(numero, pilotos.get(numero)), colorDe(pilotos.get(numero)),
                List.copyOf(tiempos.keySet()), List.copyOf(tiempos.values()))));
        return Collections.unmodifiableMap(series);
    }

    /** Los que terminaron mejor ubicados (última posición informada); sin posiciones, los primeros de la lista. */
    private static List<Integer> mejorUbicados(List<SeriePosiciones> posiciones, Map<Integer, PilotoTelemetria> pilotos) {
        if (posiciones.isEmpty()) {
            return pilotos.keySet().stream().limit(MAX_PILOTOS_VUELTAS).toList();
        }
        return posiciones.stream()
                .filter(s -> !s.posiciones().isEmpty())
                .sorted(Comparator.comparing(s -> s.posiciones().get(s.posiciones().size() - 1)))
                .limit(MAX_PILOTOS_VUELTAS)
                .map(SeriePosiciones::numero)
                .toList();
    }

    private static boolean terminada(SesionTelemetria sesion) {
        Instant ahora = Instant.now();
        if (sesion.fin() != null) {
            return sesion.fin().isBefore(ahora);
        }
        return sesion.inicio() != null && sesion.inicio().plus(DURACION_MAXIMA_SESION).isBefore(ahora);
    }

    private static SesionTelemetria aSesion(JsonNode s) {
        return new SesionTelemetria(FormatJson.aInt(s, "session_key"), FormatJson.aString(s, "session_name"),
                FormatJson.aString(s, "session_type"), FormatJson.instantUtc(FormatJson.aString(s, "date_start")),
                FormatJson.instantUtc(FormatJson.aString(s, "date_end")), FormatJson.aString(s, "country_name"),
                FormatJson.aString(s, "circuit_short_name"), FormatJson.aString(s, "location"));
    }

    private static String acronimo(Integer numero, PilotoTelemetria piloto) {
        return piloto != null && piloto.acronimo() != null ? piloto.acronimo() : String.valueOf(numero);
    }

    private static String colorDe(PilotoTelemetria piloto) {
        return piloto != null ? piloto.color() : null;
    }

    /** OpenF1 manda el color del equipo sin "#" (ej: "3671C6"). */
    static String color(String hex) {
        if (hex == null || hex.isBlank()) {
            return null;
        }
        return hex.startsWith("#") ? hex : "#" + hex;
    }
}

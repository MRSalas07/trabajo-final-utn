package com.tfi.motorsport.backend.services.telemetria.Impl;

import com.tfi.motorsport.backend.clients.clientHttpExternal.FormatJson;
import com.tfi.motorsport.backend.clients.openf1.OpenF1Cliente;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.FilaFoto;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.FotoSesion;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.PilotoTelemetria;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.SeriePosiciones;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.SerieVueltas;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.SesionTelemetria;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.TelemetriaSesion;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.Versus;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.services.telemetria.CacheTemporal;
import com.tfi.motorsport.backend.services.telemetria.CalculadoraFoto;
import com.tfi.motorsport.backend.services.telemetria.CalculadoraFoto.DatosSesion;
import com.tfi.motorsport.backend.services.telemetria.ITelemetriaService;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.Piloto;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.Posicion;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.Vuelta;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Análisis de sesiones de F1 con datos de OpenF1 (MVP 7, módulo plus).
 *
 * Diseño desacoplado: solo se crea si app.telemetria.habilitada=true, no escribe en la base
 * (consulta, transforma y guarda un rato en memoria) y no usa el resto de los servicios.
 *
 * OpenF1 da gratis los datos históricos desde 2023. Desde 30 minutos antes hasta 30 minutos después
 * de una sesión sus datos son solo para suscriptores, por eso el análisis es de sesiones ya liberadas.
 * El análisis (gráficos) y la foto usan los MISMOS datos de la sesión, guardados una vez en la caché.
 *
 * La foto de la sesión (vivo, repetición y versus) se arma con CalculadoraFoto: este servicio solo decide
 * el instante, descarga los datos (con caché, para cuidar el cupo de OpenF1) y le pasa todo. Una sesión
 * terminada se descarga una vez: después cada foto se calcula en memoria, sin consultar a OpenF1.
 */
@Service
@ConditionalOnProperty(prefix = "app.telemetria", name = "habilitada", havingValue = "true")
public class TelemetriaServiceImpl implements ITelemetriaService {

    /** OpenF1 tiene datos desde la temporada 2023. */
    public static final int PRIMER_ANIO = 2023;
    /** Pilotos por gráfico de vueltas: con más líneas se vuelve ilegible. */
    public static final int MAX_PILOTOS_VUELTAS = 6;

    private final OpenF1Cliente cliente;
    /** Sesiones terminadas: sus datos no cambian más. */
    private final CacheTemporal cache = new CacheTemporal(Duration.ofMinutes(30), 300);
    /** Sesión en curso, lo que cambia rápido: posiciones, intervalos y vueltas. */
    private final CacheTemporal cacheVivoRapido = new CacheTemporal(Duration.ofSeconds(5), 50);
    /** Sesión en curso, lo que cambia lento: pilotos, neumáticos, paradas, mensajes y clima. */
    private final CacheTemporal cacheVivoLento = new CacheTemporal(Duration.ofSeconds(30), 50);

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
                    // OpenF1 conserva las fechas canceladas (ej: 2026) marcadas con is_cancelled
                    .filter(s -> !FormatJson.booleano(s, "is_cancelled"))
                    .map(DatosOpenF1::sesion)
                    .filter(s -> s.sessionKey() != null && s.inicio() != null && s.inicio().isBefore(ahora))
                    .sorted(Comparator.comparing(SesionTelemetria::inicio).reversed())
                    .toList();
        });
    }

    @Override
    public TelemetriaSesion sesion(int sessionKey, List<Integer> pilotos) {
        SesionTelemetria sesion = buscarSesion(sessionKey);
        String motivo = DatosOpenF1.motivoSinDatosLibres(sesion, Instant.now());
        if (motivo != null) {
            return new TelemetriaSesion(sesion, List.of(), List.of(), List.of(),
                    motivo + " El análisis se habilita cuando OpenF1 libera los datos (media hora después del final).");
        }

        // Los mismos datos que usa la foto (misma caché): abrir las dos pestañas descarga la sesión una sola vez
        DatosSesion datos = datosTerminada(sessionKey);
        Map<Integer, PilotoTelemetria> porNumero = pilotosDelAnalisis(datos.pilotos());
        List<SeriePosiciones> posiciones = seriesDePosiciones(datos.posiciones(), sesion.inicio(), porNumero);
        Map<Integer, SerieVueltas> vueltas = seriesDeVueltas(datos.vueltas(), porNumero);

        List<Integer> seleccion = (pilotos == null || pilotos.isEmpty())
                ? mejorUbicados(posiciones, porNumero)
                : pilotos.stream().filter(Objects::nonNull).distinct().limit(MAX_PILOTOS_VUELTAS).toList();
        List<SerieVueltas> series = seleccion.stream().map(vueltas::get).filter(Objects::nonNull).toList();
        return new TelemetriaSesion(sesion, List.copyOf(porNumero.values()), series, posiciones, null);
    }

    // ------------------------------------------------------------------ foto de la sesión

    @Override
    public FotoSesion foto(int sessionKey, Integer vuelta, Instant instantePedido, List<Integer> pilotos) {
        List<Integer> elegidos = pilotos == null ? List.of() : pilotos.stream().filter(Objects::nonNull).distinct().toList();
        if (elegidos.size() > 2) {
            throw new RuleBusinessException("El versus compara hasta 2 pilotos");
        }
        SesionTelemetria sesion = buscarSesion(sessionKey);
        Instant ahora = Instant.now();
        if (sesion.inicio() != null && sesion.inicio().isAfter(ahora)) {
            return fotoVacia(sesion, false, "La sesión todavía no empezó.");
        }
        // Sin datos libres = la sesión está en curso (o terminó hace menos de media hora): se ve en vivo
        boolean enVivo = DatosOpenF1.motivoSinDatosLibres(sesion, ahora) != null;
        if (enVivo && !cliente.credencialesConfiguradas()) {
            return fotoVacia(sesion, true, "La sesión está en curso: OpenF1 da sus datos en vivo solo a suscriptores. "
                    + "Se va a poder ver media hora después del final.");
        }

        DatosSesion datos = enVivo ? datosEnVivo(sessionKey) : datosTerminada(sessionKey);
        Instant primerDato = CalculadoraFoto.primerInstante(datos);
        Instant ultimo = CalculadoraFoto.ultimoInstante(datos);
        if (primerDato == null) {
            return fotoVacia(sesion, enVivo, "OpenF1 todavía no tiene datos de esta sesión.");
        }
        // OpenF1 registra posiciones desde casi una hora antes (los autos van a la grilla): esa previa no se
        // recorre, la línea de tiempo empieza con el horario oficial (los datos anteriores igual cuentan para la foto)
        Instant primero = sesion.inicio() != null && sesion.inicio().isAfter(primerDato) && sesion.inicio().isBefore(ultimo)
                ? sesion.inicio() : primerDato;
        int totalVueltas = CalculadoraFoto.totalVueltas(datos.vueltas());
        Instant instante;
        if (enVivo) {
            instante = ahora;
        } else if (vuelta != null && totalVueltas > 0) {
            instante = CalculadoraFoto.finDeVuelta(datos.vueltas(), Math.min(vuelta, totalVueltas));
        } else if (instantePedido != null) {
            // La repetición pide un instante exacto: se lo acota a los datos que existen
            instante = instantePedido.isBefore(primero) ? primero : instantePedido.isAfter(ultimo) ? ultimo : instantePedido;
        } else {
            instante = ultimo;
        }

        List<FilaFoto> filas = CalculadoraFoto.filas(datos, instante);
        CalculadoraFoto.Tanda tanda = CalculadoraFoto.tanda(datos.controles(), instante);
        return new FotoSesion(sesion, enVivo, instante, primero, enVivo ? ahora : ultimo,
                esCarrera(sesion) ? CalculadoraFoto.vueltasHasta(datos.vueltas(), instante) : null,
                esCarrera(sesion) ? totalVueltas : null,
                tanda != null ? prefijoTanda(sesion) + tanda.numero() : null,
                tanda != null ? tanda.inicio() : null,
                filas, versus(datos, instante, filas, elegidos, esCarrera(sesion)),
                CalculadoraFoto.mensajes(datos.controles(), instante),
                CalculadoraFoto.clima(datos.clima(), instante),
                null);
    }

    /** Los pilotos elegidos; si falta alguno, se completa con los mejor ubicados en ese momento. */
    private static Versus versus(DatosSesion datos, Instant instante, List<FilaFoto> filas, List<Integer> elegidos,
                                 boolean compararVueltas) {
        List<Integer> numeros = new ArrayList<>(elegidos);
        for (FilaFoto fila : filas) {
            if (numeros.size() == 2) {
                break;
            }
            if (!numeros.contains(fila.numero())) {
                numeros.add(fila.numero());
            }
        }
        return numeros.size() < 2 ? null
                : CalculadoraFoto.versus(datos, instante, filas, numeros.get(0), numeros.get(1), compararVueltas);
    }

    /** Carrera y sprint se cuentan por vueltas (OpenF1 las informa con tipo "Race"); el resto, por tiempo. */
    private static boolean esCarrera(SesionTelemetria sesion) {
        return "Race".equalsIgnoreCase(sesion.tipo());
    }

    /** "Q" en la clasificación y "SQ" en la clasificación sprint. */
    private static String prefijoTanda(SesionTelemetria sesion) {
        return sesion.nombre() != null && sesion.nombre().toUpperCase(Locale.ROOT).contains("SPRINT") ? "SQ" : "Q";
    }

    /** Sesión terminada: todo se descarga una vez y se guarda media hora. */
    private DatosSesion datosTerminada(int sessionKey) {
        return cache.obtener("datos-" + sessionKey, () -> descargarDatos(sessionKey, cliente::get));
    }

    /**
     * En vivo, lo que cambia seguido (posiciones, intervalos y vueltas) se pide cada 5 s y el resto cada 30 s:
     * 3 consultas cada 5 s + 5 cada 30 s = 46 por minuto, debajo del cupo de la suscripción (55).
     */
    private DatosSesion datosEnVivo(int sessionKey) {
        String filtro = "?session_key=" + sessionKey;
        DatosSesion lento = cacheVivoLento.obtener("datos-" + sessionKey, () -> new DatosSesion(
                DatosOpenF1.pilotos(cliente.getVivo("drivers" + filtro)), List.of(), List.of(), List.of(),
                DatosOpenF1.stints(cliente.getVivo("stints" + filtro)),
                DatosOpenF1.pits(cliente.getVivo("pit" + filtro)),
                DatosOpenF1.controles(cliente.getVivo("race_control" + filtro)),
                DatosOpenF1.clima(cliente.getVivo("weather" + filtro))));
        DatosSesion rapido = cacheVivoRapido.obtener("datos-" + sessionKey, () -> new DatosSesion(List.of(),
                DatosOpenF1.posiciones(cliente.getVivo("position" + filtro)),
                DatosOpenF1.intervalos(cliente.getVivo("intervals" + filtro)),
                DatosOpenF1.vueltas(cliente.getVivo("laps" + filtro)),
                List.of(), List.of(), List.of(), List.of()));
        return new DatosSesion(lento.pilotos(), rapido.posiciones(), rapido.intervalos(), rapido.vueltas(),
                lento.stints(), lento.pits(), lento.controles(), lento.clima());
    }

    /** Todo lo de la sesión, una consulta por tipo de dato (8 en total). */
    private static DatosSesion descargarDatos(int sessionKey, Function<String, JsonNode> consultar) {
        String filtro = "?session_key=" + sessionKey;
        return new DatosSesion(
                DatosOpenF1.pilotos(consultar.apply("drivers" + filtro)),
                DatosOpenF1.posiciones(consultar.apply("position" + filtro)),
                DatosOpenF1.intervalos(consultar.apply("intervals" + filtro)),
                DatosOpenF1.vueltas(consultar.apply("laps" + filtro)),
                DatosOpenF1.stints(consultar.apply("stints" + filtro)),
                DatosOpenF1.pits(consultar.apply("pit" + filtro)),
                DatosOpenF1.controles(consultar.apply("race_control" + filtro)),
                DatosOpenF1.clima(consultar.apply("weather" + filtro)));
    }

    private static FotoSesion fotoVacia(SesionTelemetria sesion, boolean enVivo, String aviso) {
        return new FotoSesion(sesion, enVivo, null, null, null, null, null, null, null, List.of(), null, List.of(),
                null, aviso);
    }

    private SesionTelemetria buscarSesion(int sessionKey) {
        return cache.obtener("sesion-" + sessionKey, () ->
                FormatJson.lista(cliente.get("sessions?session_key=" + sessionKey)).stream()
                        .findFirst()
                        .map(DatosOpenF1::sesion)
                        .orElseThrow(() -> new ResourceNotFoundException("No existe la sesión " + sessionKey + " en OpenF1")));
    }

    // ------------------------------------------------------------------ análisis

    /**
     * Los pilotos para los gráficos, sin repetidos. Si OpenF1 no informa el color del equipo, se deja null
     * para que el gráfico use su propia paleta (y no queden varias líneas del mismo gris).
     */
    private static Map<Integer, PilotoTelemetria> pilotosDelAnalisis(List<Piloto> pilotos) {
        Map<Integer, PilotoTelemetria> porNumero = new LinkedHashMap<>();
        for (Piloto piloto : pilotos) {
            String color = DatosOpenF1.COLOR_POR_DEFECTO.equals(piloto.color()) ? null : piloto.color();
            porNumero.putIfAbsent(piloto.numero(), new PilotoTelemetria(piloto.numero(), piloto.nombre(),
                    piloto.acronimo(), piloto.equipo(), color));
        }
        return porNumero;
    }

    /** OpenF1 informa cada CAMBIO de posición con su hora (ya vienen ordenados): una serie por piloto. */
    private static List<SeriePosiciones> seriesDePosiciones(List<Posicion> cambios, Instant inicio,
                                                            Map<Integer, PilotoTelemetria> pilotos) {
        Map<Integer, SeriePosiciones> series = new LinkedHashMap<>();
        for (Posicion cambio : cambios) {
            SeriePosiciones serie = series.computeIfAbsent(cambio.numero(),
                    numero -> new SeriePosiciones(numero, acronimo(numero, pilotos.get(numero)),
                            colorDe(pilotos.get(numero)), new ArrayList<>(), new ArrayList<>()));
            long segundos = inicio == null ? 0 : Math.max(0, Duration.between(inicio, cambio.fecha()).getSeconds());
            serie.minutos().add(BigDecimal.valueOf(segundos).divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP));
            serie.posiciones().add(cambio.posicion());
        }
        return List.copyOf(series.values());
    }

    /** Las vueltas de todos los pilotos: después se elige qué pilotos mostrar sin volver a consultar. */
    private static Map<Integer, SerieVueltas> seriesDeVueltas(List<Vuelta> vueltas, Map<Integer, PilotoTelemetria> pilotos) {
        Map<Integer, TreeMap<Integer, BigDecimal>> porPiloto = new LinkedHashMap<>();
        for (Vuelta vuelta : vueltas) {
            // Las vueltas sin tiempo o de salida de boxes distorsionan el gráfico: se omiten
            if (vuelta.terminada() && !vuelta.salidaBoxes()) {
                porPiloto.computeIfAbsent(vuelta.numero(), numero -> new TreeMap<>())
                        .put(vuelta.vuelta(), vuelta.duracion().setScale(3, RoundingMode.HALF_UP));
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

    private static String acronimo(Integer numero, PilotoTelemetria piloto) {
        return piloto != null && piloto.acronimo() != null ? piloto.acronimo() : String.valueOf(numero);
    }

    private static String colorDe(PilotoTelemetria piloto) {
        return piloto != null ? piloto.color() : null;
    }
}

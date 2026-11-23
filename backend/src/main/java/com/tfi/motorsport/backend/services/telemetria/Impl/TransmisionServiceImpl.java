package com.tfi.motorsport.backend.services.telemetria.Impl;

import com.tfi.motorsport.backend.clients.clientHttpExternal.FormatJson;
import com.tfi.motorsport.backend.clients.openf1.OpenF1Cliente;
import com.tfi.motorsport.backend.config.AppProperties;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.SesionTelemetria;
import com.tfi.motorsport.backend.dtos.telemetria.TransmisionDtos.InfoVivo;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.services.telemetria.CacheTemporal;
import com.tfi.motorsport.backend.services.telemetria.ITransmisionService;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.ConFecha;
import com.tfi.motorsport.backend.services.telemetria.transmision.Repeticion;
import com.tfi.motorsport.backend.services.telemetria.transmision.Repeticion.DatosSesion;
import com.tfi.motorsport.backend.services.telemetria.transmision.Transmision;
import com.tfi.motorsport.backend.services.telemetria.transmision.Transmision.Suscripcion;
import com.tfi.motorsport.backend.services.telemetria.transmision.Vivo;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Administra las transmisiones de telemetría y sus espectadores.
 *
 *  - Cada espectador se conecta por SSE (Server-Sent Events): una conexión HTTP que queda abierta y por la
 *    que el servidor "empuja" la foto de la sesión una vez por segundo. Alcanza con SSE (y no WebSocket)
 *    porque los datos van en un solo sentido; los controles van por un PUT aparte.
 *  - REPETICIÓN: una por espectador (cada uno con su reloj, velocidad y pausa). Los datos completos de cada
 *    sesión y los tramos de telemetría se guardan en memoria y se comparten entre repeticiones.
 *  - VIVO: una por sesión, compartida por todos los espectadores.
 *  - Un único reloj (un hilo, cada 1 s) avanza todas las transmisiones y envía las fotos.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.telemetria", name = "habilitada", havingValue = "true")
public class TransmisionServiceImpl implements ITransmisionService {

    static final double VELOCIDAD_MINIMA = 0.5;
    static final double VELOCIDAD_MAXIMA = 10;
    /** Una carrera larga más margen. */
    private static final long TIMEOUT_SSE_MS = Duration.ofHours(4).toMillis();
    /** Un vivo sin espectadores se detiene después de un minuto (por si alguien recarga la página). */
    private static final Duration GRACIA_SIN_ESPECTADORES = Duration.ofSeconds(60);
    /** Tramos de telemetría compartidos entre repeticiones (cada uno, 60 s de una sesión). */
    private static final int MAX_VENTANAS_COMPARTIDAS = 120;
    /** Sesión del vivo: su número o "latest" (la actual). Se valida porque va dentro de la URL de OpenF1. */
    private static final Pattern SESION_VIVO = Pattern.compile("latest|\\d{1,7}");

    private final OpenF1Cliente cliente;
    private final AppProperties.Telemetria cfg;
    private final ScheduledExecutorService reloj = Executors.newSingleThreadScheduledExecutor(hilos("telemetria-reloj"));
    private final ScheduledExecutorService consultasVivo = Executors.newScheduledThreadPool(2, hilos("telemetria-vivo"));
    private final ExecutorService descargas = Executors.newFixedThreadPool(2, hilos("telemetria-descargas"));

    private final Map<String, Transmision> porSuscripcion = new ConcurrentHashMap<>();
    private final Map<String, Vivo> vivos = new ConcurrentHashMap<>();
    /** Datos completos de una sesión (≈4 MB por carrera): pocas sesiones a la vez. */
    private final CacheTemporal datosPorSesion = new CacheTemporal(Duration.ofMinutes(30), 4);
    /** LRU: al superar el tope se descartan los tramos usados hace más tiempo. */
    private final Map<String, List<? extends ConFecha>> ventanasCompartidas = Collections.synchronizedMap(
            new LinkedHashMap<String, List<? extends ConFecha>>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<? extends ConFecha>> masVieja) {
                    return size() > MAX_VENTANAS_COMPARTIDAS;
                }
            });
    /** El estado del vivo se guarda un minuto: cada carga de la página lo consulta. */
    private volatile InfoVivo infoVivo;
    private volatile Instant infoVivoVence = Instant.EPOCH;

    public TransmisionServiceImpl(OpenF1Cliente cliente, AppProperties props) {
        this.cliente = cliente;
        this.cfg = props.telemetria();
        reloj.scheduleAtFixedRate(this::pulso, 1, 1, TimeUnit.SECONDS);
    }

    // ------------------------------------------------------------------ alta de espectadores

    @Override
    public SseEmitter repeticion(int sessionKey, double velocidad, int inicioMin, Integer inicioVuelta, Integer piloto) {
        SseEmitter emisor = new SseEmitter(TIMEOUT_SSE_MS);
        long activas = new LinkedHashSet<>(porSuscripcion.values()).stream()
                .filter(t -> t instanceof Repeticion && !t.terminada())
                .count();
        if (activas >= Math.max(1, cfg.maxRepeticiones())) {
            return error(emisor, "Hay demasiadas repeticiones en curso. Probá de nuevo en unos minutos.");
        }
        Repeticion repeticion = new Repeticion(cliente, descargas, this::datosSesion, ventanasCompartidas, sessionKey,
                acotar(velocidad), Math.max(0, inicioMin), inicioVuelta != null && inicioVuelta >= 1 ? inicioVuelta : null);
        Suscripcion suscripcion = registrar(repeticion, emisor, piloto);
        descargas.submit(repeticion::iniciar);
        enviar(repeticion, suscripcion);
        return emisor;
    }

    @Override
    public SseEmitter vivo(String sessionKey, Integer piloto) {
        SseEmitter emisor = new SseEmitter(TIMEOUT_SSE_MS);
        if (!cliente.credencialesConfiguradas()) {
            return error(emisor, "El modo en vivo requiere la suscripción de OpenF1 (OPENF1_USUARIO y OPENF1_PASSWORD). "
                    + "Mientras tanto podés ver la repetición de las sesiones terminadas.");
        }
        String clave = sessionKey == null || sessionKey.isBlank() ? "latest" : sessionKey.trim();
        if (!SESION_VIVO.matcher(clave).matches()) {
            return error(emisor, "Sesión inválida: indicá su número o \"latest\" para la actual.");
        }
        // Una sola transmisión por sesión, compartida; si la anterior terminó (o falló), se crea otra
        Vivo vivo = vivos.compute(clave, (k, actual) -> {
            if (actual != null && !actual.terminada()) {
                return actual;
            }
            if (actual != null) {
                actual.detener();
            }
            Vivo nuevo = new Vivo(cliente, consultasVivo, k, cfg.vivoIntervaloRapidoMs(), cfg.vivoIntervaloLentoMs());
            descargas.submit(nuevo::iniciar);
            return nuevo;
        });
        Suscripcion suscripcion = registrar(vivo, emisor, piloto);
        enviar(vivo, suscripcion);
        return emisor;
    }

    // ------------------------------------------------------------------ controles

    @Override
    public void ajustar(String id, Integer piloto, Double velocidad, Boolean pausa) {
        if (velocidad != null && (velocidad.isNaN() || velocidad < VELOCIDAD_MINIMA || velocidad > VELOCIDAD_MAXIMA)) {
            throw new RuleBusinessException("La velocidad tiene que estar entre 0.5x y 10x");
        }
        Transmision transmision = porSuscripcion.get(id);
        if (transmision == null) {
            throw new ResourceNotFoundException("No existe la transmisión " + id + " (puede haber terminado)");
        }
        if (!(transmision instanceof Repeticion) && (velocidad != null || pausa != null)) {
            throw new RuleBusinessException("En vivo no se puede pausar ni cambiar la velocidad", HttpStatus.CONFLICT);
        }
        if (piloto != null) {
            // piloto 0 (o negativo) = volver a seguir al líder
            transmision.suscripciones().stream().filter(s -> s.id().equals(id)).findFirst()
                    .ifPresent(s -> s.seguir(piloto > 0 ? piloto : null));
        }
        if (transmision instanceof Repeticion repeticion) {
            if (velocidad != null) {
                repeticion.setVelocidad(velocidad);
            }
            if (pausa != null) {
                repeticion.setPausada(pausa);
            }
        }
    }

    @Override
    public InfoVivo infoVivo() {
        InfoVivo guardada = infoVivo;
        if (guardada != null && Instant.now().isBefore(infoVivoVence)) {
            return guardada;
        }
        boolean credenciales = cliente.credencialesConfiguradas();
        SesionTelemetria ultima = null;
        boolean sinRespuesta = false;
        try {
            JsonNode respuesta = credenciales ? cliente.getVivo("sessions?session_key=latest")
                    : cliente.get("sessions?session_key=latest");
            ultima = FormatJson.lista(respuesta).stream().findFirst().map(DatosOpenF1::sesion).orElse(null);
        } catch (RuntimeException ex) {
            sinRespuesta = true;
            log.debug("No se pudo consultar la última sesión de OpenF1: {}", ex.getMessage());
        }
        Instant ahora = Instant.now();
        boolean enVentana = ultima != null && ultima.inicio() != null && ultima.fin() != null
                && ahora.isAfter(ultima.inicio().minus(DatosOpenF1.DEMORA_DATOS_LIBRES))
                && ahora.isBefore(ultima.fin().plus(DatosOpenF1.DEMORA_DATOS_LIBRES));
        String mensaje;
        if (!credenciales) {
            mensaje = "El modo en vivo requiere la suscripción de OpenF1. Disponible: repetición de sesiones terminadas.";
        } else if (sinRespuesta) {
            mensaje = "No se pudo consultar OpenF1 en este momento.";
        } else if (enVentana) {
            mensaje = "¡Hay una sesión en curso!";
        } else {
            mensaje = "No hay ninguna sesión en curso en este momento.";
        }
        InfoVivo nueva = new InfoVivo(credenciales, ultima, enVentana, mensaje);
        infoVivo = nueva;
        infoVivoVence = ahora.plusSeconds(60);
        return nueva;
    }

    // ------------------------------------------------------------------ reloj y envío

    /** Cada segundo: avanza cada transmisión y envía la foto a sus espectadores. */
    private void pulso() {
        for (Transmision transmision : new LinkedHashSet<>(porSuscripcion.values())) {
            try {
                transmision.avanzar();
                for (Suscripcion suscripcion : transmision.suscripciones()) {
                    enviar(transmision, suscripcion);
                }
            } catch (RuntimeException ex) {
                log.warn("Error en una transmisión de telemetría: {}", ex.getMessage(), ex);
            }
        }
        // Vivos terminados o sin espectadores durante el minuto de gracia: se detienen
        vivos.forEach((clave, vivo) -> {
            Instant desde = vivo.sinEspectadoresDesde();
            boolean abandonado = desde != null
                    && Duration.between(desde, Instant.now()).compareTo(GRACIA_SIN_ESPECTADORES) > 0;
            if (abandonado || (vivo.terminada() && vivo.suscripciones().isEmpty())) {
                vivo.detener();
                vivos.remove(clave, vivo);
            }
        });
    }

    private void enviar(Transmision transmision, Suscripcion suscripcion) {
        try {
            suscripcion.emisor().send(SseEmitter.event().name("estado")
                    .data(transmision.foto(suscripcion), MediaType.APPLICATION_JSON));
            if (transmision.terminada()) {
                suscripcion.emisor().complete();
                quitar(transmision, suscripcion);
            }
        } catch (IOException | IllegalStateException ex) {
            // El navegador cerró la pestaña o se perdió la conexión
            quitar(transmision, suscripcion);
        }
    }

    private Suscripcion registrar(Transmision transmision, SseEmitter emisor, Integer piloto) {
        Suscripcion suscripcion = new Suscripcion(UUID.randomUUID().toString(), emisor,
                piloto != null && piloto > 0 ? piloto : null);
        transmision.agregar(suscripcion);
        porSuscripcion.put(suscripcion.id(), transmision);
        Runnable fin = () -> quitar(transmision, suscripcion);
        emisor.onCompletion(fin);
        emisor.onTimeout(fin);
        emisor.onError(e -> fin.run());
        return suscripcion;
    }

    private void quitar(Transmision transmision, Suscripcion suscripcion) {
        if (porSuscripcion.remove(suscripcion.id()) == null) {
            return; // ya se había quitado
        }
        boolean sinEspectadores = transmision.quitar(suscripcion);
        if (sinEspectadores && transmision instanceof Repeticion) {
            transmision.detener();
        }
    }

    private SseEmitter error(SseEmitter emisor, String mensaje) {
        try {
            emisor.send(SseEmitter.event().name("error").data(Map.of("mensaje", mensaje), MediaType.APPLICATION_JSON));
        } catch (IOException ignorada) {
            // sin conexión: no hay a quién avisarle
        }
        emisor.complete();
        return emisor;
    }

    static double acotar(double velocidad) {
        if (Double.isNaN(velocidad)) {
            return 1;
        }
        return Math.max(VELOCIDAD_MINIMA, Math.min(velocidad, VELOCIDAD_MAXIMA));
    }

    // ------------------------------------------------------------------ datos completos de una sesión

    /**
     * Todo lo necesario para repetir una sesión, salvo la telemetría del auto y las ubicaciones, que se
     * piden por tramos durante la reproducción. Se guarda 30 minutos y lo comparten las repeticiones.
     */
    DatosSesion datosSesion(int sessionKey) {
        return datosPorSesion.obtener("sesion-" + sessionKey, () -> descargarSesion(sessionKey));
    }

    private DatosSesion descargarSesion(int sessionKey) {
        String q = "?session_key=" + sessionKey;
        SesionTelemetria sesion = FormatJson.lista(cliente.get("sessions" + q)).stream().findFirst()
                .map(DatosOpenF1::sesion)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la sesión " + sessionKey + " en OpenF1"));
        String motivo = DatosOpenF1.motivoSinDatosLibres(sesion, Instant.now());
        if (motivo != null) {
            throw new RuleBusinessException(motivo + " Mientras tanto, se puede seguir con el modo en vivo.");
        }
        List<ConFecha> eventos = new ArrayList<>();
        eventos.addAll(DatosOpenF1.posiciones(cliente.get("position" + q)));
        eventos.addAll(DatosOpenF1.intervalos(cliente.get("intervals" + q)));
        eventos.addAll(DatosOpenF1.vueltas(cliente.get("laps" + q)));
        eventos.addAll(DatosOpenF1.controles(cliente.get("race_control" + q)));
        eventos.addAll(DatosOpenF1.clima(cliente.get("weather" + q)));
        eventos.addAll(DatosOpenF1.pits(cliente.get("pit" + q)));
        eventos.sort(Comparator.comparing(ConFecha::fecha));
        return new DatosSesion(sesion, DatosOpenF1.pilotos(cliente.get("drivers" + q)),
                DatosOpenF1.stints(cliente.get("stints" + q)), List.copyOf(eventos));
    }

    @PreDestroy
    void detener() {
        vivos.values().forEach(Vivo::detener);
        reloj.shutdownNow();
        consultasVivo.shutdownNow();
        descargas.shutdownNow();
    }

    private static ThreadFactory hilos(String nombre) {
        return tarea -> {
            Thread hilo = new Thread(tarea, nombre);
            hilo.setDaemon(true);
            return hilo;
        };
    }
}

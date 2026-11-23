package com.tfi.motorsport.backend.services.telemetria.transmision;

import com.tfi.motorsport.backend.clients.clientHttpExternal.FormatJson;
import com.tfi.motorsport.backend.clients.openf1.OpenF1Cliente;
import com.tfi.motorsport.backend.dtos.telemetria.TransmisionDtos;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.ConFecha;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * MODO EN VIVO: sigue una sesión de F1 mientras se corre (requiere la suscripción paga de OpenF1).
 *
 * Consulta OpenF1 periódicamente pidiendo solo lo NUEVO desde la última consulta (filtro date>...),
 * en dos grupos para no pasarse del cupo por minuto:
 *  - RÁPIDO (por defecto cada 8 s): intervalos, posiciones, telemetría de los autos y ubicación en pista.
 *  - LENTO (por defecto cada 30 s): vueltas, control de carrera, clima, neumáticos y boxes.
 *
 * Una única instancia por sesión es COMPARTIDA por todos los espectadores: OpenF1 se consulta una sola
 * vez sin importar cuántas personas miren, y el token nunca llega al navegador.
 */
@Slf4j
public class Vivo extends Transmision {

    /** Después del final, la transmisión sigue media hora (vueltas de enfriamiento, resultados, banderas). */
    private static final Duration MARGEN_FINAL = Duration.ofMinutes(30);

    private final OpenF1Cliente cliente;
    private final ScheduledExecutorService planificador;
    private final String sessionKeyPedida;
    private final long intervaloRapidoMs;
    private final long intervaloLentoMs;

    /** Última fecha recibida por endpoint (para pedir solo lo nuevo). Lo usan dos hilos: concurrente. */
    private final Map<String, Instant> ultimaFecha = new ConcurrentHashMap<>();
    private final List<ScheduledFuture<?>> tareas = new ArrayList<>();
    private volatile int sessionKey;
    private volatile boolean detenida;

    public Vivo(OpenF1Cliente cliente, ScheduledExecutorService planificador, String sessionKeyPedida,
                long intervaloRapidoMs, long intervaloLentoMs) {
        super(TransmisionDtos.MODO_VIVO);
        this.cliente = cliente;
        this.planificador = planificador;
        this.sessionKeyPedida = sessionKeyPedida;
        this.intervaloRapidoMs = intervaloRapidoMs;
        this.intervaloLentoMs = intervaloLentoMs;
    }

    @Override
    public void iniciar() {
        aviso = "Conectando con OpenF1 en vivo…";
        try {
            JsonNode s = FormatJson.lista(cliente.getVivo("sessions?session_key=" + sessionKeyPedida)).stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("OpenF1 no informa ninguna sesión en curso"));
            sesion = DatosOpenF1.sesion(s);
            sessionKey = sesion.sessionKey();
            estado.setPilotos(DatosOpenF1.pilotos(cliente.getVivo("drivers?session_key=" + sessionKey)));
            estado.setReloj(Instant.now());
            // Primera pasada completa: lo de la sesión hasta ahora (posiciones, banderas, vueltas)
            consultarLento();
            consultarRapido();
            programar();
            fase = EN_CURSO;
            aviso = sesion.inicio() != null && Instant.now().isBefore(sesion.inicio())
                    ? "La sesión todavía no empezó: los datos aparecen cuando arranque." : null;
        } catch (RuntimeException ex) {
            log.warn("No se pudo iniciar la transmisión en vivo: {}", ex.getMessage());
            aviso = ex.getMessage();
            fase = ERROR;
        }
    }

    private synchronized void programar() {
        if (detenida) {
            return;
        }
        tareas.add(planificador.scheduleWithFixedDelay(this::consultarRapidoSeguro,
                intervaloRapidoMs, intervaloRapidoMs, TimeUnit.MILLISECONDS));
        tareas.add(planificador.scheduleWithFixedDelay(this::consultarLentoSeguro,
                intervaloLentoMs, intervaloLentoMs, TimeUnit.MILLISECONDS));
    }

    @Override
    public void avanzar() {
        if (EN_CURSO.equals(fase) && sesion != null && sesion.fin() != null
                && Instant.now().isAfter(sesion.fin().plus(MARGEN_FINAL))) {
            fase = FINALIZADA;
            aviso = "La sesión terminó. Podés verla completa en modo repetición.";
            detener();
        }
    }

    private void consultarRapidoSeguro() {
        try {
            consultarRapido();
            if (aviso != null && aviso.startsWith("Error")) {
                aviso = null;
            }
        } catch (RuntimeException ex) {
            aviso = "Error al consultar OpenF1: " + ex.getMessage() + " (se reintenta)";
        }
    }

    private void consultarLentoSeguro() {
        try {
            consultarLento();
        } catch (RuntimeException ex) {
            aviso = "Error al consultar OpenF1: " + ex.getMessage() + " (se reintenta)";
        }
    }

    private void consultarRapido() {
        List<ConFecha> nuevos = new ArrayList<>();
        nuevos.addAll(nuevo("intervals", Duration.ofSeconds(30), DatosOpenF1::intervalos));
        nuevos.addAll(nuevo("position", null, DatosOpenF1::posiciones));
        nuevos.addAll(nuevo("car_data", Duration.ofSeconds(15), DatosOpenF1::muestrasAuto));
        nuevos.addAll(nuevo("location", Duration.ofSeconds(15), DatosOpenF1::ubicaciones));
        aplicar(nuevos);
    }

    private void consultarLento() {
        List<ConFecha> nuevos = new ArrayList<>();
        // Una vuelta se informa al empezar y se completa al terminar: se vuelven a pedir las de los últimos minutos
        Instant desdeVueltas = ultimaFecha.get("laps");
        String filtroVueltas = desdeVueltas == null ? ""
                : "&date_start>=" + OpenF1Cliente.fecha(desdeVueltas.minus(Duration.ofMinutes(3)));
        List<DatosOpenF1.Vuelta> vueltas = DatosOpenF1.vueltas(
                cliente.getVivo("laps?session_key=" + sessionKey + filtroVueltas));
        vueltas.stream().map(DatosOpenF1.Vuelta::fecha).max(Comparator.naturalOrder())
                .ifPresent(f -> ultimaFecha.put("laps", f));
        nuevos.addAll(vueltas);
        nuevos.addAll(nuevo("race_control", null, DatosOpenF1::controles));
        nuevos.addAll(nuevo("weather", Duration.ofMinutes(5), DatosOpenF1::clima));
        nuevos.addAll(nuevo("pit", null, DatosOpenF1::pits));
        estado.setStints(DatosOpenF1.stints(cliente.getVivo("stints?session_key=" + sessionKey)));
        aplicar(nuevos);
    }

    /**
     * Pide los registros nuevos de un endpoint (date > última fecha recibida).
     * La primera vez: todo (historiaInicial null) o solo los últimos segundos (datos voluminosos).
     */
    private <T extends ConFecha> List<T> nuevo(String endpoint, Duration historiaInicial,
                                               Function<JsonNode, List<T>> conversor) {
        Instant desde = ultimaFecha.get(endpoint);
        String filtro = "";
        if (desde != null) {
            filtro = "&date>" + OpenF1Cliente.fecha(desde);
        } else if (historiaInicial != null) {
            filtro = "&date>" + OpenF1Cliente.fecha(Instant.now().minus(historiaInicial));
        }
        List<T> datos = conversor.apply(cliente.getVivo(endpoint + "?session_key=" + sessionKey + filtro));
        if (!datos.isEmpty()) {
            ultimaFecha.put(endpoint, datos.get(datos.size() - 1).fecha());
        }
        return datos;
    }

    private void aplicar(List<ConFecha> nuevos) {
        nuevos.sort(Comparator.comparing(ConFecha::fecha));
        nuevos.forEach(estado::aplicar);
        nuevos.stream().map(ConFecha::fecha).max(Comparator.naturalOrder()).ifPresent(f -> {
            Instant actual = estado.reloj();
            if (actual == null || f.isAfter(actual)) {
                estado.setReloj(f);
            }
        });
    }

    @Override
    public synchronized void detener() {
        detenida = true;
        tareas.forEach(t -> t.cancel(false));
        tareas.clear();
    }
}

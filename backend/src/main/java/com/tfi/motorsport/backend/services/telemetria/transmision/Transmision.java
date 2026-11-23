package com.tfi.motorsport.backend.services.telemetria.transmision;

import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.SesionTelemetria;
import com.tfi.motorsport.backend.dtos.telemetria.TransmisionDtos.EstadoTransmision;
import com.tfi.motorsport.backend.dtos.telemetria.TransmisionDtos.FilaTorre;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Base común de una transmisión de telemetría (en vivo o repetición).
 *
 * Patrón: una FUENTE de eventos (OpenF1 en vivo o datos históricos con reloj virtual) alimenta un
 * {@link EstadoCarrera}; una vez por segundo se arma una "foto" para cada espectador conectado por SSE.
 * Así el frontend recibe exactamente el mismo formato en los dos modos.
 */
public abstract class Transmision {

    public static final String CARGANDO = "CARGANDO";
    public static final String EN_CURSO = "EN_CURSO";
    public static final String PAUSADA = "PAUSADA";
    public static final String ESPERANDO_DATOS = "ESPERANDO_DATOS";
    public static final String FINALIZADA = "FINALIZADA";
    public static final String ERROR = "ERROR";

    /** Un espectador conectado: su canal SSE y el piloto que eligió seguir (null = el líder). */
    public static final class Suscripcion {
        private final String id;
        private final SseEmitter emisor;
        private volatile Integer foco;
        /** Versión del contorno del circuito que ya recibió (se reenvía solo cuando cambia). */
        private int versionTrazadoEnviada = -1;

        public Suscripcion(String id, SseEmitter emisor, Integer foco) {
            this.id = id;
            this.emisor = emisor;
            this.foco = foco;
        }

        public String id() {
            return id;
        }

        public SseEmitter emisor() {
            return emisor;
        }

        public Integer foco() {
            return foco;
        }

        public void seguir(Integer piloto) {
            this.foco = piloto;
        }
    }

    protected final String modo;
    protected final EstadoCarrera estado = new EstadoCarrera();
    protected final List<Suscripcion> suscripciones = new CopyOnWriteArrayList<>();
    protected volatile SesionTelemetria sesion;
    protected volatile String fase = CARGANDO;
    protected volatile String aviso;
    protected volatile double velocidad = 1;
    protected volatile boolean pausada;
    private volatile Instant sinEspectadoresDesde;

    protected Transmision(String modo) {
        this.modo = modo;
    }

    /** Carga inicial (se ejecuta en segundo plano). */
    public abstract void iniciar();

    /** Avanza la transmisión un paso (lo llama el reloj cada segundo). */
    public abstract void avanzar();

    /** Libera recursos (consultas pendientes o programadas). */
    public void detener() {
    }

    public boolean terminada() {
        return FINALIZADA.equals(fase) || ERROR.equals(fase);
    }

    public String fase() {
        return fase;
    }

    public String aviso() {
        return aviso;
    }

    public EstadoCarrera estado() {
        return estado;
    }

    // ------------------------------------------------------------------ espectadores

    public void agregar(Suscripcion suscripcion) {
        suscripciones.add(suscripcion);
        sinEspectadoresDesde = null;
    }

    /** Quita un espectador. Devuelve true si la transmisión quedó sin espectadores. */
    public boolean quitar(Suscripcion suscripcion) {
        suscripciones.remove(suscripcion);
        if (suscripciones.isEmpty()) {
            if (sinEspectadoresDesde == null) {
                sinEspectadoresDesde = Instant.now();
            }
            return true;
        }
        return false;
    }

    public List<Suscripcion> suscripciones() {
        return suscripciones;
    }

    /** Desde cuándo no tiene espectadores (null si tiene). */
    public Instant sinEspectadoresDesde() {
        return sinEspectadoresDesde;
    }

    // ------------------------------------------------------------------ foto

    /** Arma la foto para un espectador (su piloto elegido y, si cambió, el contorno del circuito). */
    public EstadoTransmision foto(Suscripcion s) {
        Instant reloj = estado.reloj();
        SesionTelemetria actual = sesion;
        Instant inicio = actual != null ? actual.inicio() : null;
        Integer foco = s.foco;
        if (foco == null) {
            foco = estado.torre().stream().filter(f -> f.posicion() < 99).findFirst()
                    .map(FilaTorre::numero).orElse(null);
        }
        int version = estado.versionTrazado();
        List<double[]> trazado = null;
        if (version != s.versionTrazadoEnviada) {
            trazado = estado.trazado();
            s.versionTrazadoEnviada = version;
        }
        return new EstadoTransmision(s.id, modo,
                actual != null ? actual.sessionKey() : null,
                actual != null ? actual.nombre() : null,
                actual != null ? actual.circuito() : null,
                actual != null ? actual.pais() : null,
                reloj, inicio,
                reloj != null && inicio != null ? (int) Duration.between(inicio, reloj).toMinutes() : null,
                estado.vueltaLider(), estado.estadoPista(),
                pausada && !terminada() ? PAUSADA : fase, velocidad,
                estado.torre(), estado.mensajes(), estado.clima(), estado.foco(foco), estado.mapa(), trazado, aviso);
    }
}

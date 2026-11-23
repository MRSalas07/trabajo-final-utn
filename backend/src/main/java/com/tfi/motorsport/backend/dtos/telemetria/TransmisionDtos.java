package com.tfi.motorsport.backend.dtos.telemetria;

import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.SesionTelemetria;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Transmisión de telemetría F1 (MVP 7): lo que recibe el frontend una vez por segundo, por SSE,
 * durante una transmisión (en vivo o repetición).
 * El formato es el MISMO en ambos modos: el frontend no distingue de dónde vienen los datos.
 */
public final class TransmisionDtos {

    private TransmisionDtos() {
    }

    public static final String MODO_VIVO = "VIVO";
    public static final String MODO_REPETICION = "REPETICION";

    /** Estado de la sesión en el instante "hora" (reloj real en vivo, reloj virtual en repetición). */
    public record EstadoTransmision(
            String id,
            String modo,
            Integer sessionKey,
            String sesion,
            String circuito,
            String pais,
            Instant hora,
            Instant inicioSesion,
            /** Minutos transcurridos desde el inicio de la sesión. */
            Integer minuto,
            /** Vuelta del líder. */
            Integer vuelta,
            /** VERDE, AMARILLA, SC, VSC, ROJA, CUADROS. */
            String estadoPista,
            /** CARGANDO, EN_CURSO, PAUSADA, ESPERANDO_DATOS, FINALIZADA. */
            String estado,
            double velocidad,
            List<FilaTorre> torre,
            List<MensajeControl> mensajes,
            Clima clima,
            Foco foco,
            List<PuntoMapa> mapa,
            /** Contorno del circuito (x, y). Se envía solo cuando cambia; el frontend conserva el último. */
            List<double[]> trazado,
            String aviso
    ) {
    }

    /** Fila de la torre de tiempos. */
    public record FilaTorre(
            int posicion,
            int numero,
            String acronimo,
            String nombre,
            String equipo,
            String color,
            /** Diferencia con el líder: "+12.345", "+1 VUELTA" o "Líder". */
            String gap,
            /** Diferencia con el auto de adelante. */
            String intervalo,
            BigDecimal ultimaVuelta,
            BigDecimal mejorVuelta,
            Integer vueltas,
            String neumatico,
            Integer edadNeumatico,
            boolean enBoxes,
            /** true si tiene la vuelta más rápida de la sesión hasta el momento. */
            boolean vueltaRapida
    ) {
    }

    public record MensajeControl(Instant hora, Integer vuelta, String bandera, String categoria, String mensaje) {
    }

    public record Clima(BigDecimal aire, BigDecimal pista, Integer humedad, boolean lluvia, BigDecimal viento) {
    }

    /** Telemetría del piloto elegido (a 3.7 Hz en origen; se envía la última muestra y una traza corta). */
    public record Foco(
            int numero,
            String acronimo,
            String color,
            Integer velocidad,
            Integer rpm,
            Integer marcha,
            Integer acelerador,
            Integer freno,
            boolean drs,
            List<Integer> trazaVelocidad,
            List<Integer> trazaAcelerador,
            List<Integer> trazaFreno
    ) {
    }

    public record PuntoMapa(int numero, String acronimo, String color, double x, double y) {
    }

    /** Estado del modo en vivo: si hay credenciales y si ahora mismo hay una sesión en curso. */
    public record InfoVivo(boolean credenciales, SesionTelemetria ultimaSesion, boolean enVentanaVivo, String mensaje) {
    }
}

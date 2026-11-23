package com.tfi.motorsport.backend.services.telemetria;

import com.tfi.motorsport.backend.dtos.telemetria.TransmisionDtos.InfoVivo;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Transmisiones de telemetría F1 por SSE (MVP 7, módulo plus): cada espectador recibe una "foto" de la
 * sesión por segundo (evento "estado"). Si no se puede transmitir, recibe un evento "error" con el motivo.
 * Solo existe si app.telemetria.habilitada=true.
 */
public interface ITransmisionService {

    /**
     * Repetición de una sesión terminada como si fuera en vivo (datos gratuitos de OpenF1).
     *
     * @param velocidad    x1 = tiempo real (entre 0.5 y 10)
     * @param inicioMin    minuto de la sesión desde el que se empieza
     * @param inicioVuelta vuelta desde la que se empieza (si se indica, manda sobre inicioMin)
     * @param piloto       número del piloto a seguir (null = el líder)
     */
    SseEmitter repeticion(int sessionKey, double velocidad, int inicioMin, Integer inicioVuelta, Integer piloto);

    /** Transmisión EN VIVO de una sesión ("latest" = la actual). Requiere la suscripción de OpenF1. */
    SseEmitter vivo(String sessionKey, Integer piloto);

    /** Cambia el piloto seguido, la velocidad o la pausa de una transmisión (velocidad y pausa: solo repetición). */
    void ajustar(String id, Integer piloto, Double velocidad, Boolean pausa);

    /** Si el modo en vivo está configurado y si ahora hay una sesión en curso. */
    InfoVivo infoVivo();
}

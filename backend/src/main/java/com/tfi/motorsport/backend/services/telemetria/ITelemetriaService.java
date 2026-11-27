package com.tfi.motorsport.backend.services.telemetria;

import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.FotoSesion;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.SesionTelemetria;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.TelemetriaSesion;

import java.time.Instant;
import java.util.List;

/**
 * Telemetría F1 (MVP 7, módulo plus) a partir de los datos públicos de OpenF1.
 * Solo existe si app.telemetria.habilitada=true.
 */
public interface ITelemetriaService {

    /** Sesiones de un año (desde 2023) que ya empezaron, de la más reciente a la más vieja. */
    List<SesionTelemetria> sesiones(int anio);

    /**
     * Análisis de una sesión terminada: pilotos, tiempos por vuelta y evolución de posiciones.
     *
     * @param pilotos números de auto para el gráfico de vueltas (vacío = los mejor ubicados, hasta 6)
     */
    TelemetriaSesion sesion(int sessionKey, List<Integer> pilotos);

    /**
     * Cómo estaba la sesión en un instante: torre, versus de dos pilotos, mensajes y clima.
     * Sesión en curso: el instante es ahora (requiere la suscripción de OpenF1).
     * Sesión terminada: el final de la vuelta pedida del líder, el instante pedido o, sin ninguno, el final.
     *
     * @param pilotos hasta 2 números de auto para el versus (si falta alguno, se completa con los mejor ubicados)
     */
    FotoSesion foto(int sessionKey, Integer vuelta, Instant instante, List<Integer> pilotos);
}

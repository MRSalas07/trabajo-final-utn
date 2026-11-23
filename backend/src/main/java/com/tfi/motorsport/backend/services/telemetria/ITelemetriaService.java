package com.tfi.motorsport.backend.services.telemetria;

import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.SesionTelemetria;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.TelemetriaSesion;

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
}

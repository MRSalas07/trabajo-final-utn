package com.tfi.motorsport.backend.dtos.telemetria;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTOs del módulo de telemetría F1 (MVP 7). No se guardan en la base: se arman con datos de OpenF1
 * y se mantienen un rato en memoria.
 */
public final class TelemetriaDtos {

    private TelemetriaDtos() {
    }

    /** Una sesión de OpenF1 (práctica, clasificación, sprint o carrera). */
    public record SesionTelemetria(Integer sessionKey, String nombre, String tipo, Instant inicio, Instant fin,
                                   String pais, String circuito, String ubicacion) {
    }

    /** color: el color del equipo en hexadecimal ("#3671C6"), para los gráficos. */
    public record PilotoTelemetria(Integer numero, String nombre, String acronimo, String equipo, String color) {
    }

    /** Tiempos de vuelta de un piloto: vueltas[i] -> tiempos[i] en segundos (sin vueltas de salida de boxes). */
    public record SerieVueltas(Integer numero, String acronimo, String color,
                               List<Integer> vueltas, List<BigDecimal> tiempos) {
    }

    /** Posición a lo largo de la sesión: minutos[i] desde el inicio -> posiciones[i]. */
    public record SeriePosiciones(Integer numero, String acronimo, String color,
                                  List<BigDecimal> minutos, List<Integer> posiciones) {
    }

    /** aviso: por qué no hay datos (ej: la sesión todavía no terminó), o null. */
    public record TelemetriaSesion(SesionTelemetria sesion, List<PilotoTelemetria> pilotos,
                                   List<SerieVueltas> vueltas, List<SeriePosiciones> posiciones,
                                   String aviso) {
    }
}

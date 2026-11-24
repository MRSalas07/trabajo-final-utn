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

    // ------------------------------------------------------------------ foto de la sesión en un instante

    /**
     * Fila de la torre en el instante de la foto. diferencia: con el líder ("+1.234", "+1 VUELTA"), o con la
     * mejor vuelta del primero si la sesión no es una carrera. Tiempos en segundos.
     */
    public record FilaFoto(Integer posicion, int numero, String acronimo, String nombre, String equipo, String color,
                           String diferencia, String intervalo, Integer vueltas, BigDecimal ultimaVuelta,
                           BigDecimal mejorVuelta, boolean vueltaRapida, String neumatico, Integer edadNeumatico,
                           int paradas) {
    }

    /** Una vuelta con sus 3 sectores (segundos) y las velocidades (km/h) en los intermedios y en la trampa. */
    public record VueltaDetalle(Integer vuelta, BigDecimal tiempo, BigDecimal sector1, BigDecimal sector2,
                                BigDecimal sector3, Integer velocidadI1, Integer velocidadI2, Integer velocidadTrampa) {
    }

    /**
     * La vuelta que el piloto está dando en el instante de la foto: solo aparecen los sectores que ya completó
     * (con la velocidad de su intermedio). segundos: cuánto lleva de vuelta.
     */
    public record VueltaEnCurso(int vuelta, long segundos, BigDecimal sector1, Integer velocidadI1,
                                BigDecimal sector2, Integer velocidadI2) {
    }

    /**
     * Un piloto del versus hasta el instante de la foto. mejoresSectores: su mejor tiempo en cada sector
     * (aunque sean de vueltas distintas). tiempoEnBoxes: lo que tardó en la calle de boxes en su última parada.
     * foto: dirección de la foto oficial (el navegador la carga directo desde Fórmula 1).
     */
    public record PilotoVersus(int numero, String acronimo, String nombre, String equipo, String color, String foto,
                               Integer posicion, Integer vueltas, VueltaEnCurso vueltaEnCurso,
                               VueltaDetalle ultimaVuelta, VueltaDetalle mejorVuelta,
                               List<BigDecimal> mejoresSectores, Integer velocidadMaxima, int paradas,
                               BigDecimal tiempoEnBoxes, String neumatico, Integer edadNeumatico) {
    }

    /** La misma vuelta de los dos pilotos: quién fue más rápido y por cuánto (en segundos, "0.234"). */
    public record VueltaComparada(int vuelta, BigDecimal tiempoA, BigDecimal tiempoB, Integer masRapido,
                                  String diferencia) {
    }

    /**
     * Dos pilotos frente a frente. mejoresSectoresSesion: el mejor tiempo de cada sector entre TODOS los
     * pilotos hasta el instante (se marca en violeta, como en la TV). vueltas: las últimas, la más reciente primero.
     */
    public record Versus(PilotoVersus a, PilotoVersus b, List<BigDecimal> mejoresSectoresSesion,
                         List<VueltaComparada> vueltas) {
    }

    /** Mensaje de la dirección de carrera (en inglés, como lo publica la FIA). */
    public record MensajeCarrera(Instant fecha, Integer vuelta, String bandera, String mensaje) {
    }

    /** Temperaturas en °C, humedad en % y viento en m/s. */
    public record ClimaFoto(BigDecimal aire, BigDecimal pista, Integer humedad, boolean lluvia, BigDecimal viento) {
    }

    /**
     * Cómo estaba la sesión en un instante: cada dato es el último que OpenF1 informó hasta ese momento.
     * inicioDatos y finDatos: entre qué instantes hay datos (para la línea de tiempo).
     * vuelta: vueltas que completó el líder (solo carrera y sprint). tanda: "Q1", "Q2", "Q3" ("SQ1"... en la
     * sprint) en la clasificación, con su hora de inicio. aviso: por qué no hay datos, o null.
     */
    public record FotoSesion(SesionTelemetria sesion, boolean enVivo, Instant instante, Instant inicioDatos,
                             Instant finDatos, Integer vuelta, Integer totalVueltas, String tanda, Instant inicioTanda,
                             List<FilaFoto> filas, Versus versus, List<MensajeCarrera> mensajes, ClimaFoto clima,
                             String aviso) {
    }
}

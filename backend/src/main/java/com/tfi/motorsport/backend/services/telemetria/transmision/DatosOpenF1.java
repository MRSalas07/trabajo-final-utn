package com.tfi.motorsport.backend.services.telemetria.transmision;

import com.tfi.motorsport.backend.clients.clientHttpExternal.FormatJson;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.SesionTelemetria;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Registros de OpenF1 ya convertidos a tipos Java (lectura tolerante: lo inválido se descarta).
 * Son los mismos para datos históricos y en vivo, porque OpenF1 usa el mismo formato en ambos casos.
 */
public final class DatosOpenF1 {

    /** Color para los pilotos cuyo equipo no informa color. */
    static final String COLOR_POR_DEFECTO = "#9aa6b5";
    /**
     * Los datos de una sesión son solo para suscriptores desde 30 minutos antes hasta 30 minutos después
     * de la sesión: recién ahí quedan libres (gratis) para el análisis y la repetición.
     */
    public static final Duration DEMORA_DATOS_LIBRES = Duration.ofMinutes(30);
    /** Si OpenF1 no informa el fin de una sesión, se la da por terminada 4 horas después de empezar. */
    private static final Duration DURACION_MAXIMA_SESION = Duration.ofHours(4);

    private DatosOpenF1() {
    }

    /** Todo evento tiene un instante: la transmisión lo aplica cuando su reloj lo alcanza. */
    public interface ConFecha {
        Instant fecha();
    }

    public record Piloto(int numero, String acronimo, String nombre, String equipo, String color) {
    }

    public record Posicion(Instant fecha, int numero, int posicion) implements ConFecha {
    }

    public record Intervalo(Instant fecha, int numero, String gap, String intervalo) implements ConFecha {
    }

    /** Vuelta completada: se aplica en el instante en que termina (inicio + duración). */
    public record Vuelta(Instant fecha, int numero, int vuelta, BigDecimal duracion, boolean salidaBoxes)
            implements ConFecha {
        /** Hora en que el piloto empezó la vuelta ("fecha" es cuando la terminó). */
        public Instant inicio() {
            return duracion == null ? fecha : fecha.minusMillis(milisegundos(duracion));
        }
    }

    public record Control(Instant fecha, Integer vuelta, String bandera, String categoria, String mensaje,
                          String alcance) implements ConFecha {
    }

    public record ClimaMuestra(Instant fecha, BigDecimal aire, BigDecimal pista, Integer humedad, boolean lluvia,
                               BigDecimal viento) implements ConFecha {
    }

    public record Pit(Instant fecha, int numero, Integer vuelta) implements ConFecha {
    }

    public record Stint(int numero, int vueltaInicio, Integer vueltaFin, String compuesto, Integer edadInicial) {
    }

    public record MuestraAuto(Instant fecha, int numero, Integer velocidad, Integer rpm, Integer marcha,
                              Integer acelerador, Integer freno, Integer drs) implements ConFecha {
    }

    public record Ubicacion(Instant fecha, int numero, double x, double y) implements ConFecha {
    }

    // ------------------------------------------------------------------ conversión desde JSON

    /** Una sesión de OpenF1 (la usan el análisis, la repetición y el vivo). */
    public static SesionTelemetria sesion(JsonNode s) {
        return new SesionTelemetria(FormatJson.aInt(s, "session_key"), FormatJson.aString(s, "session_name"),
                FormatJson.aString(s, "session_type"), fecha(s, "date_start"), fecha(s, "date_end"),
                FormatJson.aString(s, "country_name"), FormatJson.aString(s, "circuit_short_name"),
                FormatJson.aString(s, "location"));
    }

    /** Por qué los datos de la sesión todavía no son libres (sin suscripción), o null si ya se pueden pedir. */
    public static String motivoSinDatosLibres(SesionTelemetria sesion, Instant ahora) {
        Instant fin = sesion.fin() != null ? sesion.fin()
                : sesion.inicio() != null ? sesion.inicio().plus(DURACION_MAXIMA_SESION) : null;
        if (fin == null) {
            return "OpenF1 no informa el horario de la sesión.";
        }
        if (fin.isAfter(ahora)) {
            return "La sesión todavía no terminó.";
        }
        if (fin.plus(DEMORA_DATOS_LIBRES).isAfter(ahora)) {
            return "La sesión terminó hace menos de 30 minutos: OpenF1 libera sus datos media hora después del final.";
        }
        return null;
    }

    public static List<Piloto> pilotos(JsonNode arreglo) {
        return convertir(arreglo, n -> {
            Integer numero = FormatJson.aInt(n, "driver_number");
            if (numero == null) {
                return null;
            }
            String color = FormatJson.aString(n, "team_colour");
            return new Piloto(numero, FormatJson.aString(n, "name_acronym"), FormatJson.aString(n, "full_name"),
                    FormatJson.aString(n, "team_name"),
                    color == null ? COLOR_POR_DEFECTO : (color.startsWith("#") ? color : "#" + color));
        }, null);
    }

    public static List<Posicion> posiciones(JsonNode arreglo) {
        return convertir(arreglo, n -> {
            Instant fecha = fecha(n, "date");
            Integer numero = FormatJson.aInt(n, "driver_number");
            Integer posicion = FormatJson.aInt(n, "position");
            return fecha == null || numero == null || posicion == null ? null : new Posicion(fecha, numero, posicion);
        }, Comparator.comparing(Posicion::fecha));
    }

    public static List<Intervalo> intervalos(JsonNode arreglo) {
        return convertir(arreglo, n -> {
            Instant fecha = fecha(n, "date");
            Integer numero = FormatJson.aInt(n, "driver_number");
            return fecha == null || numero == null ? null
                    : new Intervalo(fecha, numero, diferencia(n.get("gap_to_leader")), diferencia(n.get("interval")));
        }, Comparator.comparing(Intervalo::fecha));
    }

    /** OpenF1 informa cada vuelta con su hora de INICIO: se aplica cuando termina (inicio + duración). */
    public static List<Vuelta> vueltas(JsonNode arreglo) {
        return convertir(arreglo, n -> {
            Instant inicio = fecha(n, "date_start");
            Integer numero = FormatJson.aInt(n, "driver_number");
            Integer vuelta = FormatJson.aInt(n, "lap_number");
            if (inicio == null || numero == null || vuelta == null) {
                return null;
            }
            BigDecimal duracion = FormatJson.aDecimal(n, "lap_duration");
            Instant fin = duracion != null ? inicio.plusMillis(milisegundos(duracion)) : inicio;
            return new Vuelta(fin, numero, vuelta, duracion, FormatJson.booleano(n, "is_pit_out_lap"));
        }, Comparator.comparing(Vuelta::fecha));
    }

    public static List<Control> controles(JsonNode arreglo) {
        return convertir(arreglo, n -> {
            Instant fecha = fecha(n, "date");
            return fecha == null ? null : new Control(fecha, FormatJson.aInt(n, "lap_number"),
                    FormatJson.aString(n, "flag"), FormatJson.aString(n, "category"),
                    FormatJson.aString(n, "message"), FormatJson.aString(n, "scope"));
        }, Comparator.comparing(Control::fecha));
    }

    public static List<ClimaMuestra> clima(JsonNode arreglo) {
        return convertir(arreglo, n -> {
            Instant fecha = fecha(n, "date");
            if (fecha == null) {
                return null;
            }
            Integer lluvia = FormatJson.aInt(n, "rainfall");
            BigDecimal humedad = FormatJson.aDecimal(n, "humidity");
            return new ClimaMuestra(fecha, FormatJson.aDecimal(n, "air_temperature"),
                    FormatJson.aDecimal(n, "track_temperature"), humedad != null ? humedad.intValue() : null,
                    lluvia != null && lluvia > 0, FormatJson.aDecimal(n, "wind_speed"));
        }, Comparator.comparing(ClimaMuestra::fecha));
    }

    public static List<Pit> pits(JsonNode arreglo) {
        return convertir(arreglo, n -> {
            Instant fecha = fecha(n, "date");
            Integer numero = FormatJson.aInt(n, "driver_number");
            return fecha == null || numero == null ? null : new Pit(fecha, numero, FormatJson.aInt(n, "lap_number"));
        }, Comparator.comparing(Pit::fecha));
    }

    public static List<Stint> stints(JsonNode arreglo) {
        return convertir(arreglo, n -> {
            Integer numero = FormatJson.aInt(n, "driver_number");
            Integer inicio = FormatJson.aInt(n, "lap_start");
            return numero == null || inicio == null ? null
                    : new Stint(numero, inicio, FormatJson.aInt(n, "lap_end"), FormatJson.aString(n, "compound"),
                    FormatJson.aInt(n, "tyre_age_at_start"));
        }, Comparator.comparing(Stint::vueltaInicio));
    }

    public static List<MuestraAuto> muestrasAuto(JsonNode arreglo) {
        return convertir(arreglo, n -> {
            Instant fecha = fecha(n, "date");
            Integer numero = FormatJson.aInt(n, "driver_number");
            return fecha == null || numero == null ? null
                    : new MuestraAuto(fecha, numero, FormatJson.aInt(n, "speed"), FormatJson.aInt(n, "rpm"),
                    FormatJson.aInt(n, "n_gear"), FormatJson.aInt(n, "throttle"), FormatJson.aInt(n, "brake"),
                    FormatJson.aInt(n, "drs"));
        }, Comparator.comparing(MuestraAuto::fecha));
    }

    public static List<Ubicacion> ubicaciones(JsonNode arreglo) {
        return convertir(arreglo, n -> {
            Instant fecha = fecha(n, "date");
            Integer numero = FormatJson.aInt(n, "driver_number");
            BigDecimal x = FormatJson.aDecimal(n, "x");
            BigDecimal y = FormatJson.aDecimal(n, "y");
            // (0,0) aparece cuando el auto no está en pista o no hay señal: se descarta
            if (fecha == null || numero == null || x == null || y == null || (x.signum() == 0 && y.signum() == 0)) {
                return null;
            }
            return new Ubicacion(fecha, numero, x.doubleValue(), y.doubleValue());
        }, Comparator.comparing(Ubicacion::fecha));
    }

    // ------------------------------------------------------------------ utilidades

    /** OpenF1 informa las diferencias como número (segundos) o texto ("+1 LAP"); el líder viene en null. */
    static String diferencia(JsonNode valor) {
        if (valor == null || valor.isNull() || valor.isMissingNode()) {
            return null;
        }
        if (valor.isNumber()) {
            return String.format(Locale.ROOT, "+%.3f", valor.decimalValue());
        }
        String texto = valor.asString().trim();
        return texto.replace("LAPS", "VUELTAS").replace("LAP", "VUELTA");
    }

    private static Instant fecha(JsonNode nodo, String campo) {
        return FormatJson.instantUtc(FormatJson.aString(nodo, campo));
    }

    private static long milisegundos(BigDecimal segundos) {
        return segundos.multiply(BigDecimal.valueOf(1000)).longValue();
    }

    private static <T> List<T> convertir(JsonNode arreglo, Function<JsonNode, T> conversor, Comparator<T> orden) {
        List<T> lista = new ArrayList<>();
        for (JsonNode nodo : FormatJson.lista(arreglo)) {
            T valor = conversor.apply(nodo);
            if (valor != null) {
                lista.add(valor);
            }
        }
        if (orden != null) {
            lista.sort(orden);
        }
        return List.copyOf(lista);
    }
}

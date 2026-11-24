package com.tfi.motorsport.backend.services.telemetria;

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
    public static final String COLOR_POR_DEFECTO = "#9aa6b5";
    /**
     * Los datos de una sesión son solo para suscriptores desde 30 minutos antes hasta 30 minutos después
     * de la sesión: recién ahí quedan libres (gratis) para el análisis y la línea de tiempo.
     */
    public static final Duration DEMORA_DATOS_LIBRES = Duration.ofMinutes(30);
    /** Si OpenF1 no informa el fin de una sesión, se la da por terminada 4 horas después de empezar. */
    private static final Duration DURACION_MAXIMA_SESION = Duration.ofHours(4);

    private DatosOpenF1() {
    }

    /** Todo registro tiene un instante: la foto de la sesión en el instante T usa solo los registros con fecha <= T. */
    public interface ConFecha {
        Instant fecha();
    }

    /** foto: dirección de la foto oficial del piloto (la sirve Fórmula 1; acá solo se guarda el enlace). */
    public record Piloto(int numero, String acronimo, String nombre, String equipo, String color, String foto) {

        public Piloto(int numero, String acronimo, String nombre, String equipo, String color) {
            this(numero, acronimo, nombre, equipo, color, null);
        }
    }

    public record Posicion(Instant fecha, int numero, int posicion) implements ConFecha {
    }

    public record Intervalo(Instant fecha, int numero, String gap, String intervalo) implements ConFecha {
    }

    /**
     * Vuelta completada: cuenta desde el instante en que termina (inicio + duración).
     * Trae los tiempos de los 3 sectores y las velocidades (km/h) en los dos intermedios y en la trampa de velocidad.
     */
    public record Vuelta(Instant fecha, int numero, int vuelta, BigDecimal duracion, boolean salidaBoxes,
                         BigDecimal sector1, BigDecimal sector2, BigDecimal sector3,
                         Integer velocidadI1, Integer velocidadI2, Integer velocidadTrampa) implements ConFecha {

        public Vuelta(Instant fecha, int numero, int vuelta, BigDecimal duracion, boolean salidaBoxes) {
            this(fecha, numero, vuelta, duracion, salidaBoxes, null, null, null, null, null, null);
        }

        /**
         * Tiene tiempo: el piloto la completó. En vivo OpenF1 crea la vuelta cuando empieza (sin tiempo) y le va
         * agregando los sectores; también quedan sin tiempo la vuelta en la que un piloto abandona.
         */
        public boolean terminada() {
            return duracion != null;
        }

        /** Hora en que el piloto empezó la vuelta ("fecha" es cuando la terminó). */
        public Instant inicio() {
            return duracion == null ? fecha : fecha.minusMillis(milisegundos(duracion));
        }
    }

    /** tanda: 1, 2 o 3 en la clasificación (qualifying_phase de OpenF1); null en el resto de las sesiones. */
    public record Control(Instant fecha, Integer vuelta, String bandera, String categoria, String mensaje,
                          String alcance, Integer tanda) implements ConFecha {

        public Control(Instant fecha, Integer vuelta, String bandera, String categoria, String mensaje, String alcance) {
            this(fecha, vuelta, bandera, categoria, mensaje, alcance, null);
        }
    }

    public record ClimaMuestra(Instant fecha, BigDecimal aire, BigDecimal pista, Integer humedad, boolean lluvia,
                               BigDecimal viento) implements ConFecha {
    }

    /** duracion: segundos desde que entró hasta que salió de la calle de boxes. */
    public record Pit(Instant fecha, int numero, Integer vuelta, BigDecimal duracion) implements ConFecha {

        public Pit(Instant fecha, int numero, Integer vuelta) {
            this(fecha, numero, vuelta, null);
        }
    }

    public record Stint(int numero, int vueltaInicio, Integer vueltaFin, String compuesto, Integer edadInicial) {
    }


    // ------------------------------------------------------------------ conversión desde JSON

    /** Una sesión de OpenF1 (la usan el análisis y la foto de la sesión). */
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
                    color == null ? COLOR_POR_DEFECTO : (color.startsWith("#") ? color : "#" + color),
                    FormatJson.aString(n, "headshot_url"));
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
            return new Vuelta(fin, numero, vuelta, duracion, FormatJson.booleano(n, "is_pit_out_lap"),
                    FormatJson.aDecimal(n, "duration_sector_1"), FormatJson.aDecimal(n, "duration_sector_2"),
                    FormatJson.aDecimal(n, "duration_sector_3"), FormatJson.aInt(n, "i1_speed"),
                    FormatJson.aInt(n, "i2_speed"), FormatJson.aInt(n, "st_speed"));
        }, Comparator.comparing(Vuelta::fecha));
    }

    public static List<Control> controles(JsonNode arreglo) {
        return convertir(arreglo, n -> {
            Instant fecha = fecha(n, "date");
            return fecha == null ? null : new Control(fecha, FormatJson.aInt(n, "lap_number"),
                    FormatJson.aString(n, "flag"), FormatJson.aString(n, "category"),
                    FormatJson.aString(n, "message"), FormatJson.aString(n, "scope"),
                    FormatJson.aInt(n, "qualifying_phase"));
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
            return fecha == null || numero == null ? null
                    : new Pit(fecha, numero, FormatJson.aInt(n, "lap_number"), FormatJson.aDecimal(n, "pit_duration"));
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

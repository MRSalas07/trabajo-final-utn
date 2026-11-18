package com.tfi.motorsport.backend.services.Impl;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Genera archivos iCalendar (.ics, RFC 5545) que el celular o el calendario de escritorio
 * ofrecen agregar al abrirlos. Todas las horas se escriben en UTC: cada app las muestra en su zona.
 *
 * Reglas del formato que se respetan:
 * - fin de línea CRLF;
 * - líneas de más de 75 bytes se pliegan (la continuación empieza con un espacio), sin cortar caracteres UTF-8;
 * - en los textos se escapan '\', ';', ',' y los saltos de línea.
 */
public final class GeneradorIcs {

    /** Duración que se asume cuando una sesión no tiene hora de fin cargada. */
    static final long DURACION_POR_DEFECTO_MINUTOS = 120;

    private static final DateTimeFormatter FORMATO_UTC =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final String CRLF = "\r\n";
    private static final int MAX_BYTES_LINEA = 75;

    /**
     * Un evento del calendario.
     * @param uid        identificador estable: si se vuelve a importar, la app actualiza en lugar de duplicar
     * @param fin        puede ser null: se usa inicio + {@link #DURACION_POR_DEFECTO_MINUTOS}
     * @param cancelado  se marca STATUS:CANCELLED
     */
    public record EventoIcs(String uid, String titulo, Instant inicio, Instant fin,
                            String lugar, String descripcion, boolean cancelado) {
    }

    private GeneradorIcs() {
    }

    /**
     * @param nombreCalendario nombre que muestran algunas apps al importar (X-WR-CALNAME)
     * @param ahora            marca de generación (DTSTAMP)
     */
    public static String generar(String nombreCalendario, List<EventoIcs> eventos, Instant ahora) {
        StringBuilder ics = new StringBuilder();
        linea(ics, "BEGIN:VCALENDAR");
        linea(ics, "VERSION:2.0");
        linea(ics, "PRODID:-//Motorsport Analytics//Calendario//ES");
        linea(ics, "CALSCALE:GREGORIAN");
        linea(ics, "METHOD:PUBLISH");
        linea(ics, "X-WR-CALNAME:" + escapar(nombreCalendario));
        String marca = FORMATO_UTC.format(ahora.truncatedTo(ChronoUnit.SECONDS));
        for (EventoIcs e : eventos) {
            Instant fin = e.fin() != null && e.fin().isAfter(e.inicio()) ? e.fin()
                    : e.inicio().plus(DURACION_POR_DEFECTO_MINUTOS, ChronoUnit.MINUTES);
            linea(ics, "BEGIN:VEVENT");
            linea(ics, "UID:" + e.uid());
            linea(ics, "DTSTAMP:" + marca);
            linea(ics, "DTSTART:" + FORMATO_UTC.format(e.inicio()));
            linea(ics, "DTEND:" + FORMATO_UTC.format(fin));
            linea(ics, "SUMMARY:" + escapar(e.titulo()));
            if (e.lugar() != null && !e.lugar().isBlank()) {
                linea(ics, "LOCATION:" + escapar(e.lugar()));
            }
            if (e.descripcion() != null && !e.descripcion().isBlank()) {
                linea(ics, "DESCRIPTION:" + escapar(e.descripcion()));
            }
            if (e.cancelado()) {
                linea(ics, "STATUS:CANCELLED");
            }
            linea(ics, "END:VEVENT");
        }
        linea(ics, "END:VCALENDAR");
        return ics.toString();
    }

    /** Escapa un valor de texto según RFC 5545 (sección 3.3.11). */
    static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }

    /** Agrega una línea plegándola cada 75 bytes; nunca corta un carácter UTF-8 por la mitad. */
    private static void linea(StringBuilder ics, String contenido) {
        int bytes = 0;
        int limite = MAX_BYTES_LINEA;
        for (int i = 0; i < contenido.length(); ) {
            int codigo = contenido.codePointAt(i);
            String caracter = new String(Character.toChars(codigo));
            int largo = caracter.getBytes(StandardCharsets.UTF_8).length;
            if (bytes + largo > limite) {
                ics.append(CRLF).append(' ');
                bytes = 0;
                limite = MAX_BYTES_LINEA - 1; // el espacio inicial de la continuación ocupa un byte
            }
            ics.append(caracter);
            bytes += largo;
            i += Character.charCount(codigo);
        }
        ics.append(CRLF);
    }
}

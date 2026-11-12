package com.tfi.motorsport.backend.services;

import com.tfi.motorsport.backend.common.Horario;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Conversión de horarios (módulo de calendario).
 *
 * Convención del sistema: en la base todo se guarda en UTC. Al responder, cada horario
 * se informa en UTC y en la zona del usuario. La zona se toma, en este orden, de:
 *   1) el parámetro ?zona= de la petición (el frontend envía la del navegador),
 *   2) la preferencia guardada en el perfil del usuario,
 *   3) UTC.
 */
public final class ZonaHoraria {

    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private ZonaHoraria() {
    }

    /** Valida una zona IANA. Lanza error 400 con un mensaje claro si no existe. */
    public static ZoneId resolverZonaHoraria(String zona) {
        if (zona == null || zona.isBlank()) {
            return ZoneId.of("UTC");
        }
        try {
            return ZoneId.of(zona.trim());
        } catch (DateTimeException ex) {
            throw new RuleBusinessException("Zona horaria inválida: '" + zona
                    + "'. Use un identificador IANA, por ejemplo 'America/Argentina/Buenos_Aires'.");
        }
    }

    public static boolean esValida(String zona) {
        try {
            ZoneId.of(zona);
            return true;
        } catch (DateTimeException | NullPointerException ex) {
            return false;
        }
    }

    public static Horario horario(Instant instante, ZoneId zona) {
        if (instante == null) {
            return null;
        }
        ZonedDateTime z = instante.atZone(zona);
        return new Horario(instante, z.toLocalDateTime().format(LOCAL), zona.getId(), z.getOffset().getId()
                .replace("Z", "+00:00"));
    }

    /** Hora local en la sede del evento (ej: "2025-03-16T15:00:00" en Melbourne). */
    public static String horaSede(Instant instante, String zonaSede) {
        if (instante == null) {
            return null;
        }
        ZoneId zona = esValida(zonaSede) ? ZoneId.of(zonaSede) : ZoneId.of("UTC");
        return instante.atZone(zona).toLocalDateTime().format(LOCAL);
    }

    /**
     * Diferencia horaria entre la sede y el usuario en un instante dado, en minutos.
     * Se calcula en la fecha del evento porque los horarios de verano cambian la diferencia
     * (ej: Argentina - Australia es 13 h o 14 h según la época del año).
     */
    public static Integer diferenciaMinutos(Instant instante, String zonaSede, ZoneId zonaUsuario) {
        if (instante == null) {
            return null;
        }
        ZoneId sede = esValida(zonaSede) ? ZoneId.of(zonaSede) : ZoneId.of("UTC");
        int seg = sede.getRules().getOffset(instante).getTotalSeconds()
                - zonaUsuario.getRules().getOffset(instante).getTotalSeconds();
        return seg / 60;
    }

    /** Texto legible: "La sede está 14 h adelantada respecto de tu hora" / "3 h 30 min atrasada" / "misma hora". */
    public static String textoDiferencia(Integer minutos) {
        if (minutos == null) {
            return null;
        }
        if (minutos == 0) {
            return "La sede tiene la misma hora que vos";
        }
        int abs = Math.abs(minutos);
        String cantidad = (abs / 60 > 0 ? (abs / 60) + " h" : "") + (abs % 60 > 0 ? (abs / 60 > 0 ? " " : "") + (abs % 60) + " min" : "");
        return "La sede está " + cantidad + (minutos > 0 ? " adelantada" : " atrasada") + " respecto de tu hora";
    }
}

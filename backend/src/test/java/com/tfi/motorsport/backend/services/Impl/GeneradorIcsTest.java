package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.services.Impl.GeneradorIcs.EventoIcs;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeneradorIcsTest {

    private static final Instant AHORA = Instant.parse("2026-11-17T12:00:00Z");

    private static EventoIcs evento(String titulo, Instant fin, boolean cancelado) {
        return new EventoIcs("sesion-1@motorsport-analytics", titulo, Instant.parse("2025-03-16T04:00:00Z"), fin,
                "Albert Park Circuit, Melbourne, Australia", "Gran Premio de Australia", cancelado);
    }

    @Test
    void generaUnCalendarioValidoConFinDeLineaCrlfYHorasEnUtc() {
        String ics = GeneradorIcs.generar("F1 2025", List.of(evento("F1 · Australia · Carrera",
                Instant.parse("2025-03-16T06:00:00Z"), false)), AHORA);

        assertThat(ics).startsWith("BEGIN:VCALENDAR\r\nVERSION:2.0\r\n").endsWith("END:VCALENDAR\r\n");
        assertThat(ics.replace("\r\n", "")).doesNotContain("\n");
        assertThat(ics).contains("DTSTART:20250316T040000Z\r\n", "DTEND:20250316T060000Z\r\n",
                "DTSTAMP:20261117T120000Z\r\n", "UID:sesion-1@motorsport-analytics\r\n");
    }

    @Test
    void escapaComasPuntoYComaYBarrasEnLosTextos() {
        String ics = GeneradorIcs.generar("F1", List.of(evento("A; B, C \\ D", null, false)), AHORA);

        assertThat(ics).contains("SUMMARY:A\\; B\\, C \\\\ D\r\n");
        assertThat(ics).contains("LOCATION:Albert Park Circuit\\, Melbourne\\, Australia\r\n");
    }

    @Test
    void sinHoraDeFin_usaLaDuracionPorDefecto() {
        String ics = GeneradorIcs.generar("F1", List.of(evento("Carrera", null, false)), AHORA);

        assertThat(ics).contains("DTEND:20250316T060000Z\r\n"); // 04:00 + 120 minutos
    }

    @Test
    void eventoCancelado_seMarcaComoCancelado() {
        String ics = GeneradorIcs.generar("TC", List.of(evento("TC · Centenario · Final", null, true)), AHORA);

        assertThat(ics).contains("STATUS:CANCELLED\r\n");
    }

    @Test
    void pliegaLasLineasLargasSinPasarLos75BytesNiCortarCaracteres() {
        String titulo = "TC · Autódromo Parque Provincia del Neuquén · Centenario · Neuquén · Argentina · Final";

        String ics = GeneradorIcs.generar("TC 2025", List.of(evento(titulo, null, false)), AHORA);

        String[] lineas = ics.split("\r\n");
        assertThat(Arrays.stream(lineas).mapToInt(l -> l.getBytes(StandardCharsets.UTF_8).length).max().orElse(0))
                .isLessThanOrEqualTo(75);
        assertThat(Arrays.stream(lineas).anyMatch(l -> l.startsWith(" "))).isTrue();
        // Al desplegar (quitar CRLF + espacio) el texto original queda intacto
        assertThat(ics.replace("\r\n ", "")).contains("SUMMARY:" + titulo);
    }
}

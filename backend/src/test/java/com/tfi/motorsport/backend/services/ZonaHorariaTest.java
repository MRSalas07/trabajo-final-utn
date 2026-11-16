package com.tfi.motorsport.backend.services;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ZonaHorariaTest {

    @Test
    void textoDiferencia_conHorasYMinutos_incluyeLosMinutos() {
        assertThat(ZonaHoraria.textoDiferencia(510))
                .isEqualTo("La sede está 8 h 30 min adelantada respecto de tu hora");
        assertThat(ZonaHoraria.textoDiferencia(-210))
                .isEqualTo("La sede está 3 h 30 min atrasada respecto de tu hora");
    }

    @Test
    void textoDiferencia_soloHorasOSoloMinutos() {
        assertThat(ZonaHoraria.textoDiferencia(840))
                .isEqualTo("La sede está 14 h adelantada respecto de tu hora");
        assertThat(ZonaHoraria.textoDiferencia(-30))
                .isEqualTo("La sede está 30 min atrasada respecto de tu hora");
    }

    @Test
    void textoDiferencia_sinDiferenciaOSinDato() {
        assertThat(ZonaHoraria.textoDiferencia(0)).isEqualTo("La sede tiene la misma hora que vos");
        assertThat(ZonaHoraria.textoDiferencia(null)).isNull();
    }

    @Test
    void diferenciaMinutos_sedeAdelantadaEsPositiva() {
        // 16/03/2025: Melbourne está en UTC+11 (horario de verano) y Buenos Aires en UTC-3
        Instant inicio = Instant.parse("2025-03-16T04:00:00Z");

        Integer minutos = ZonaHoraria.diferenciaMinutos(inicio, "Australia/Melbourne",
                ZoneId.of("America/Argentina/Buenos_Aires"));

        assertThat(minutos).isEqualTo(840);
    }
}

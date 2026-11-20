package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.dtos.perfil.ResultadoRecienteDto;
import com.tfi.motorsport.backend.repositories.unificacion.FilaResultado;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultadosRecientesTest {

    private static FilaResultado fila(long eventoId, String inicio, String tipo, Integer posicion) {
        return new FilaResultado(eventoId, 1, "Evento " + eventoId, Instant.parse(inicio), 1L, 2025, "F1",
                1L, "Lando", "Norris", "NOR", 10L, "McLaren", tipo, posicion, BigDecimal.ONE, "FINALIZADO");
    }

    @Test
    void soloCuentanCarrerasYClasificacionesGenerales_delMasRecienteAlMasAntiguo() {
        List<FilaResultado> filas = List.of(
                fila(1, "2025-03-16T04:00:00Z", "CARRERA", 2),
                fila(1, "2025-03-16T04:00:00Z", "CLASIFICACION", 1),
                fila(2, "2025-03-23T07:00:00Z", "SPRINT", 1),
                fila(3, "2025-04-06T05:00:00Z", "GENERAL", 3),
                fila(4, "2025-04-13T05:00:00Z", "ETAPA", 1));

        List<ResultadoRecienteDto> recientes = ResultadosRecientes.de(filas, true);

        assertThat(recientes).extracting(ResultadoRecienteDto::eventoId).containsExactly(3L, 1L);
        assertThat(recientes.getFirst().categoria()).isEqualTo("F1");
    }

    @Test
    void enLaFichaDelPiloto_quedaUnResultadoPorEvento() {
        List<FilaResultado> filas = List.of(
                fila(1, "2025-03-16T04:00:00Z", "CARRERA", 5),
                fila(1, "2025-03-16T04:00:00Z", "CARRERA", 1));

        assertThat(ResultadosRecientes.de(filas, true)).extracting(ResultadoRecienteDto::posicion).containsExactly(1);
        assertThat(ResultadosRecientes.de(filas, false)).extracting(ResultadoRecienteDto::posicion).containsExactly(1, 5);
    }

    @Test
    void devuelveComoMaximoDiezResultados() {
        List<FilaResultado> filas = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            filas.add(fila(i, "2025-03-16T04:00:00Z", "CARRERA", 1));
        }

        assertThat(ResultadosRecientes.de(filas, true)).hasSize(ResultadosRecientes.LIMITE);
    }
}

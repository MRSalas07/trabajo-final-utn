package com.tfi.motorsport.backend.services.estadisticas;

import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.*;
import com.tfi.motorsport.backend.services.estadisticas.MotorEstadistico.Participacion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tfi.motorsport.backend.services.estadisticas.CampeonatoDePrueba.*;
import static org.assertj.core.api.Assertions.assertThat;

class AnalisisHistoricoTest {

    private static List<Participacion> participacionesDeDosTemporadas() {
        return MotorEstadistico.consolidar(dosTemporadas());
    }

    @Test
    void historicoDePiloto_armaUnaFilaPorTemporada() {
        Historico historico = AnalisisHistorico.historicoPiloto(participacionesDeDosTemporadas(), HAMILTON, "F1");

        assertThat(historico.nombre()).isEqualTo("Lewis Hamilton");
        assertThat(historico.temporadas()).extracting(TemporadaHistorica::anio).containsExactly(2025, 2026);
        assertThat(historico.temporadas()).extracting(TemporadaHistorica::posicionCampeonato).containsExactly(3, 1);
        assertThat(historico.puntosTotales()).as("51 en 2025 + 50 en 2026").isEqualByComparingTo("101");
        assertThat(historico.mejorPosicionCampeonato()).isEqualTo(1);
        assertThat(historico.variacionPuntosPorEvento()).as("de 17 a 25 puntos por evento").isEqualByComparingTo("47.1");
    }

    @Test
    void historicoDeEquipo_calculaLaPosicionDeCadaTemporada() {
        Historico historico = AnalisisHistorico.historicoEquipo(participacionesDeDosTemporadas(), EQUIPO_ROJO, "F1");

        assertThat(historico.temporadas()).extracting(TemporadaHistorica::posicionCampeonato).containsExactly(1, 2);
        assertThat(historico.temporadas().get(0).detalle()).contains("Fernando Alonso").contains("Franco Colapinto");
        assertThat(historico.temporadas().get(1).detalle()).isEqualTo("Fernando Alonso");
    }

    @Test
    void comparativaHistorica_cuentaTemporadasYCarrerasDelante() {
        ComparativaHistorica comparativa = AnalisisHistorico.compararPilotos(
                participacionesDeDosTemporadas(), ALONSO, HAMILTON, "F1");

        assertThat(comparativa.anios()).containsExactly(2025, 2026);
        assertThat(comparativa.posicionesA()).containsExactly(1, 2);
        assertThat(comparativa.posicionesB()).containsExactly(3, 1);
        assertThat(comparativa.temporadasDelanteA()).isEqualTo(1);
        assertThat(comparativa.temporadasDelanteB()).isEqualTo(1);
        assertThat(comparativa.carrerasDelanteA()).isEqualTo(2);
        assertThat(comparativa.carrerasDelanteB()).isEqualTo(3);
    }
}

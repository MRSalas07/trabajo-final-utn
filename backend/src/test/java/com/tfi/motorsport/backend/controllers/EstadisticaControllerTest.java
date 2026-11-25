package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.ComparativaHistorica;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.ComparativaPilotos;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.EvolucionCampeonato;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.EvolucionGrupos;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.FilaCampeonatoEquipo;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.FilaCampeonatoPiloto;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.FilaMarca;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.Historico;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.ResumenTemporada;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.TendenciaGrupo;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.TendenciaPiloto;
import com.tfi.motorsport.backend.services.IEstadisticaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del controlador de estadísticas.
 * Se llama directamente a los métodos del controlador con el servicio simulado (mock):
 * se verifica que cada endpoint delegue en el método correcto del servicio con sus parámetros.
 * Los cálculos se prueban en el motor estadístico y en EstadisticaServiceImplTest;
 * los límites de los parámetros (año, top, tipo) se prueban en EstadisticasYEventosRutasTest.
 * Cada prueba sigue el patrón AAA: Arrange (preparar), Act (ejecutar), Assert (verificar).
 */
@ExtendWith(MockitoExtension.class)
class EstadisticaControllerTest {

    @Mock
    private IEstadisticaService estadisticaService;

    @InjectMocks
    private EstadisticaController controller;

    @Test
    void resumen_devuelveElResumenDeLaTemporada() {
        // Arrange
        ResumenTemporada resumen = mock(ResumenTemporada.class);
        when(estadisticaService.resumen("F1", 2025)).thenReturn(resumen);

        // Act
        ResumenTemporada resultado = controller.resumen("F1", 2025);

        // Assert
        assertThat(resultado).isSameAs(resumen);
    }

    @Test
    void pilotos_devuelveElCampeonatoDePilotos() {
        // Arrange
        FilaCampeonatoPiloto fila = mock(FilaCampeonatoPiloto.class);
        when(estadisticaService.campeonatoPilotos("F1", 2025)).thenReturn(List.of(fila));

        // Act
        List<FilaCampeonatoPiloto> resultado = controller.pilotos("F1", 2025);

        // Assert
        assertThat(resultado).containsExactly(fila);
    }

    @Test
    void equipos_devuelveElCampeonatoDeEquipos() {
        // Arrange
        FilaCampeonatoEquipo fila = mock(FilaCampeonatoEquipo.class);
        when(estadisticaService.campeonatoEquipos("WRC", 2026)).thenReturn(List.of(fila));

        // Act
        List<FilaCampeonatoEquipo> resultado = controller.equipos("WRC", 2026);

        // Assert
        assertThat(resultado).containsExactly(fila);
    }

    @Test
    void evolucion_pasaLosPilotosElegidosYElTop() {
        // Arrange
        EvolucionCampeonato evolucion = mock(EvolucionCampeonato.class);
        when(estadisticaService.evolucionPilotos("F1", 2025, List.of(1L, 2L), 5)).thenReturn(evolucion);

        // Act
        EvolucionCampeonato resultado = controller.evolucion("F1", 2025, List.of(1L, 2L), 5);

        // Assert
        assertThat(resultado).isSameAs(evolucion);
    }

    @Test
    void tendencia_devuelveLaTendenciaDelPiloto() {
        // Arrange
        TendenciaPiloto tendencia = mock(TendenciaPiloto.class);
        when(estadisticaService.tendenciaPiloto("F1", 2025, 3L)).thenReturn(tendencia);

        // Act
        TendenciaPiloto resultado = controller.tendencia("F1", 2025, 3L);

        // Assert
        assertThat(resultado).isSameAs(tendencia);
    }

    @Test
    void comparativa_comparaLosDosPilotosIndicados() {
        // Arrange
        ComparativaPilotos comparativa = mock(ComparativaPilotos.class);
        when(estadisticaService.compararPilotos("F1", 2025, 1L, 2L)).thenReturn(comparativa);

        // Act
        ComparativaPilotos resultado = controller.comparativa("F1", 2025, 1L, 2L);

        // Assert
        assertThat(resultado).isSameAs(comparativa);
    }

    @Test
    void recalcular_devuelveLaCategoriaEnMayusculasYLaCantidadDePilotosActualizados() {
        // Arrange
        when(estadisticaService.recalcularPalmares("f1")).thenReturn(24);

        // Act
        Map<String, Object> resultado = controller.recalcular("f1");

        // Assert
        assertThat(resultado)
                .containsEntry("categoria", "F1")
                .containsEntry("pilotosActualizados", 24);
    }

    @Test
    void marcas_devuelveElRankingDeMarcas() {
        // Arrange
        FilaMarca fila = mock(FilaMarca.class);
        when(estadisticaService.rankingMarcas("TC", 2025)).thenReturn(List.of(fila));

        // Act
        List<FilaMarca> resultado = controller.marcas("TC", 2025);

        // Assert
        assertThat(resultado).containsExactly(fila);
    }

    @Test
    void tendenciaEquipo_devuelveLaTendenciaDelEquipo() {
        // Arrange
        TendenciaGrupo tendencia = mock(TendenciaGrupo.class);
        when(estadisticaService.tendenciaEquipo("F1", 2025, 10L)).thenReturn(tendencia);

        // Act
        TendenciaGrupo resultado = controller.tendenciaEquipo("F1", 2025, 10L);

        // Assert
        assertThat(resultado).isSameAs(tendencia);
    }

    @Test
    void tendenciaMarca_devuelveLaTendenciaDeLaMarca() {
        // Arrange
        TendenciaGrupo tendencia = mock(TendenciaGrupo.class);
        when(estadisticaService.tendenciaMarca("TC", 2025, "Ford")).thenReturn(tendencia);

        // Act
        TendenciaGrupo resultado = controller.tendenciaMarca("TC", 2025, "Ford");

        // Assert
        assertThat(resultado).isSameAs(tendencia);
    }

    @Test
    void comparativaHistorica_comparaLosDosPilotosEnTodasLasTemporadas() {
        // Arrange
        ComparativaHistorica comparativa = mock(ComparativaHistorica.class);
        when(estadisticaService.comparativaHistorica("F1", 1L, 2L)).thenReturn(comparativa);

        // Act
        ComparativaHistorica resultado = controller.comparativaHistorica("F1", 1L, 2L);

        // Assert
        assertThat(resultado).isSameAs(comparativa);
    }

    @Test
    void evolucionGrupos_pasaElTipoYElTop() {
        // Arrange
        EvolucionGrupos evolucion = mock(EvolucionGrupos.class);
        when(estadisticaService.evolucionGrupos("F1", 2025, "MARCA", 6)).thenReturn(evolucion);

        // Act
        EvolucionGrupos resultado = controller.evolucionGrupos("F1", 2025, "MARCA", 6);

        // Assert
        assertThat(resultado).isSameAs(evolucion);
    }

    @Test
    void historicoPiloto_devuelveElHistorialDelPiloto() {
        // Arrange
        Historico historico = mock(Historico.class);
        when(estadisticaService.historicoPiloto("F1", 1L)).thenReturn(historico);

        // Act
        Historico resultado = controller.historicoPiloto("F1", 1L);

        // Assert
        assertThat(resultado).isSameAs(historico);
    }

    @Test
    void historicoEquipo_devuelveElHistorialDelEquipo() {
        // Arrange
        Historico historico = mock(Historico.class);
        when(estadisticaService.historicoEquipo("F1", 10L)).thenReturn(historico);

        // Act
        Historico resultado = controller.historicoEquipo("F1", 10L);

        // Assert
        assertThat(resultado).isSameAs(historico);
    }
}

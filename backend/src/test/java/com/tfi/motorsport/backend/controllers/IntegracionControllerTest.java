package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.TemporadaNorm;
import com.tfi.motorsport.backend.dtos.integracionAPI.CargaSincronizacionDto;
import com.tfi.motorsport.backend.dtos.integracionAPI.CargaSincronizacionDto.SolicitudSincronizacion;
import com.tfi.motorsport.backend.dtos.integracionAPI.ResumenSincronizacion;
import com.tfi.motorsport.backend.services.ISincronizacionService;
import com.tfi.motorsport.backend.services.ISincronizacionService.FuenteDisponible;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del controlador de integración (sincronización con fuentes externas).
 * Se llama directamente a los métodos del controlador con el servicio simulado (mock).
 * La regla de que el trabajo se encola y no se espera (202) se verifica sobre la respuesta.
 * Cada prueba sigue el patrón AAA: Arrange (preparar), Act (ejecutar), Assert (verificar).
 */
@ExtendWith(MockitoExtension.class)
class IntegracionControllerTest {

    @Mock
    private ISincronizacionService sincronizacion;

    @InjectMocks
    private IntegracionController controller;

    @Test
    void fuentes_devuelveLasFuentesDisponibles() {
        // Arrange
        FuenteDisponible f1 = new FuenteDisponible("F1", "Jolpica", true);
        when(sincronizacion.fuentes()).thenReturn(List.of(f1));

        // Act
        List<FuenteDisponible> resultado = controller.fuentes();

        // Assert
        assertThat(resultado).containsExactly(f1);
    }

    @Test
    void sincronizar_encolaElTrabajoYRespondeAccepted() {
        // Arrange
        CargaSincronizacionDto trabajo = mock(CargaSincronizacionDto.class);
        when(sincronizacion.iniciar("F1", 2025)).thenReturn(trabajo);

        // Act
        ResponseEntity<CargaSincronizacionDto> respuesta = controller.sincronizar(new SolicitudSincronizacion("F1", 2025));

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(respuesta.getBody()).isSameAs(trabajo);
    }

    @Test
    void trabajos_devuelveElHistorialDeTrabajos() {
        // Arrange
        CargaSincronizacionDto trabajo = mock(CargaSincronizacionDto.class);
        when(sincronizacion.trabajos()).thenReturn(List.of(trabajo));

        // Act
        List<CargaSincronizacionDto> resultado = controller.trabajos();

        // Assert
        assertThat(resultado).containsExactly(trabajo);
    }

    @Test
    void estado_devuelveElAvanceDelTrabajoIndicado() {
        // Arrange
        CargaSincronizacionDto trabajo = mock(CargaSincronizacionDto.class);
        when(sincronizacion.estado("abc-123")).thenReturn(trabajo);

        // Act
        CargaSincronizacionDto resultado = controller.estado("abc-123");

        // Assert
        assertThat(resultado).isSameAs(trabajo);
    }

    @Test
    void importar_guardaLaTemporadaYDevuelveElResumen() {
        // Arrange
        TemporadaNorm temporada = mock(TemporadaNorm.class);
        ResumenSincronizacion resumen = mock(ResumenSincronizacion.class);
        when(sincronizacion.importar(temporada)).thenReturn(resumen);

        // Act
        ResumenSincronizacion resultado = controller.importar(temporada);

        // Assert
        assertThat(resultado).isSameAs(resumen);
    }
}

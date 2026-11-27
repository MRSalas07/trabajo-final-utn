package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.dtos.evento.EventoDto;
import com.tfi.motorsport.backend.dtos.evento.TablaEtapaDto;
import com.tfi.motorsport.backend.dtos.sesion.TablaSesionDto;
import com.tfi.motorsport.backend.dtos.temporada.TemporadaDto;
import com.tfi.motorsport.backend.services.IEventoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del controlador de eventos y resultados.
 * Se llama directamente a los métodos del controlador con el servicio simulado (mock):
 * se verifica que delegue en el servicio y que le pase la zona horaria ya resuelta.
 * Los límites del año (1950 a 2100) se prueban en EstadisticasYEventosRutasTest.
 * Cada prueba sigue el patrón AAA: Arrange (preparar), Act (ejecutar), Assert (verificar).
 */
@ExtendWith(MockitoExtension.class)
class EventoControllerTest {

    private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");

    @Mock
    private IEventoService eventoService;

    @Mock
    private ZonaHorariaSolicitada zonaHoraria;

    @InjectMocks
    private EventoController controller;

    @Test
    void temporadas_devuelveLasTemporadasDeLaCategoria() {
        // Arrange
        TemporadaDto temporada = mock(TemporadaDto.class);
        when(eventoService.temporadasDeCategoria("F1")).thenReturn(List.of(temporada));

        // Act
        List<TemporadaDto> resultado = controller.temporadas("F1");

        // Assert
        assertThat(resultado).containsExactly(temporada);
    }

    @Test
    void eventos_resuelveLaZonaDelParametroYSeLaPasaAlServicio() {
        // Arrange
        EventoDto evento = mock(EventoDto.class);
        when(zonaHoraria.resolverZonaHoraria("America/Argentina/Buenos_Aires")).thenReturn(BUENOS_AIRES);
        when(eventoService.eventosDeTemporada("F1", 2025, BUENOS_AIRES)).thenReturn(List.of(evento));

        // Act
        List<EventoDto> resultado = controller.eventos("F1", 2025, "America/Argentina/Buenos_Aires");

        // Assert
        assertThat(resultado).containsExactly(evento);
        verify(zonaHoraria).resolverZonaHoraria("America/Argentina/Buenos_Aires");
    }

    @Test
    void eventos_sinZonaEnElPedido_dejaQueElResolutorElijaLaPorDefecto() {
        // Arrange
        ZoneId utc = ZoneId.of("UTC");
        when(zonaHoraria.resolverZonaHoraria(null)).thenReturn(utc);
        when(eventoService.eventosDeTemporada("WRC", 2026, utc)).thenReturn(List.of());

        // Act
        List<EventoDto> resultado = controller.eventos("WRC", 2026, null);

        // Assert
        assertThat(resultado).isEmpty();
        verify(eventoService).eventosDeTemporada("WRC", 2026, utc);
    }

    @Test
    void evento_devuelveElDetalleEnLaZonaPedida() {
        // Arrange
        EventoDto detalle = mock(EventoDto.class);
        ZoneId madrid = ZoneId.of("Europe/Madrid");
        when(zonaHoraria.resolverZonaHoraria("Europe/Madrid")).thenReturn(madrid);
        when(eventoService.detalle(9L, madrid)).thenReturn(detalle);

        // Act
        EventoDto resultado = controller.evento(9L, "Europe/Madrid");

        // Assert
        assertThat(resultado).isSameAs(detalle);
    }

    @Test
    void resultadosSesion_devuelveLaTablaDeLaSesion() {
        // Arrange
        TablaSesionDto tabla = mock(TablaSesionDto.class);
        ZoneId utc = ZoneId.of("UTC");
        when(zonaHoraria.resolverZonaHoraria(null)).thenReturn(utc);
        when(eventoService.resultadosDeSesion(4L, utc)).thenReturn(tabla);

        // Act
        TablaSesionDto resultado = controller.resultadosSesion(4L, null);

        // Assert
        assertThat(resultado).isSameAs(tabla);
    }

    @Test
    void resultadosEtapa_devuelveLaTablaDeLaEtapa() {
        // Arrange
        TablaEtapaDto tabla = mock(TablaEtapaDto.class);
        ZoneId utc = ZoneId.of("UTC");
        when(zonaHoraria.resolverZonaHoraria(null)).thenReturn(utc);
        when(eventoService.resultadosDeEtapa(6L, utc)).thenReturn(tabla);

        // Act
        TablaEtapaDto resultado = controller.resultadosEtapa(6L, null);

        // Assert
        assertThat(resultado).isSameAs(tabla);
    }
}

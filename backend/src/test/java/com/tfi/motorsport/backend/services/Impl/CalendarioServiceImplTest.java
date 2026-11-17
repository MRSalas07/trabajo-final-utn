package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.dtos.calendario.ArchivoIcs;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.model.Evento;
import com.tfi.motorsport.backend.model.Sesion;
import com.tfi.motorsport.backend.model.Temporada;
import com.tfi.motorsport.backend.repositories.EventoJPARepository;
import com.tfi.motorsport.backend.repositories.SesionJPARepository;
import com.tfi.motorsport.backend.repositories.TemporadaJPARepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarioServiceImplTest {

    @Mock private EventoJPARepository eventoRepo;
    @Mock private SesionJPARepository sesionRepo;
    @Mock private TemporadaJPARepository temporadaRepo;

    @InjectMocks
    private CalendarioServiceImpl service;

    private static Temporada temporada(String siglas) {
        Categoria c = new Categoria();
        c.setId(1L);
        c.setSiglas(siglas);
        Temporada t = new Temporada();
        t.setId(10L);
        t.setAnio(2025);
        t.setCategoria(c);
        return t;
    }

    private static Evento evento(Long id, Temporada t, String nombre, String nombreCorto) {
        Evento e = new Evento();
        e.setId(id);
        e.setTemporada(t);
        e.setNombre(nombre);
        e.setNombreCorto(nombreCorto);
        e.setCircuito("Albert Park Circuit");
        e.setCiudad("Melbourne");
        e.setPais("Australia");
        e.setFechaInicio(Instant.parse("2025-03-14T00:00:00Z"));
        e.setFechaFin(Instant.parse("2025-03-16T23:59:59Z"));
        e.setEstado("FINALIZADO");
        return e;
    }

    private static Sesion sesion(Long id, Evento e, String nombre, String tipo, String inicio) {
        Sesion s = new Sesion();
        s.setId(id);
        s.setEvento(e);
        s.setNombre(nombre);
        s.setTipo(tipo);
        s.setFechaInicio(inicio != null ? Instant.parse(inicio) : null);
        s.setEstado("FINALIZADO");
        return s;
    }

    private static int cantidadDeEventos(String ics) {
        return ics.split("BEGIN:VEVENT", -1).length - 1;
    }

    @Test
    void icsDeEvento_conSesiones_generaUnaEntradaPorSesionConFecha() {
        // Arrange
        Evento australia = evento(1L, temporada("F1"), "Gran Premio de Australia", "Australia");
        when(eventoRepo.buscarConCategoria(1L)).thenReturn(Optional.of(australia));
        when(sesionRepo.listarPorEventos(List.of(1L))).thenReturn(List.of(
                sesion(11L, australia, "Clasificación", "CLASIFICACION", "2025-03-15T05:00:00Z"),
                sesion(12L, australia, "Carrera", "CARRERA", "2025-03-16T04:00:00Z"),
                sesion(13L, australia, "Práctica sin fecha", "PRACTICA", null)));

        // Act
        ArchivoIcs archivo = service.icsDeEvento(1L);

        // Assert
        assertThat(archivo.nombreArchivo()).isEqualTo("f1-2025-australia.ics");
        assertThat(cantidadDeEventos(archivo.contenido())).isEqualTo(2);
        assertThat(archivo.contenido()).contains("SUMMARY:F1 · Australia · Carrera", "UID:sesion-12@motorsport-analytics");
    }

    @Test
    void icsDeEvento_sinSesiones_generaUnaSolaEntradaParaTodoElEvento() {
        // Arrange: un rally de WRC no tiene sesiones, solo etapas
        Evento rally = evento(2L, temporada("WRC"), "Rallye Automobile Monte-Carlo", "Monte-Carlo");
        when(eventoRepo.buscarConCategoria(2L)).thenReturn(Optional.of(rally));

        // Act
        ArchivoIcs archivo = service.icsDeEvento(2L);

        // Assert
        assertThat(archivo.nombreArchivo()).isEqualTo("wrc-2025-monte-carlo.ics");
        assertThat(cantidadDeEventos(archivo.contenido())).isEqualTo(1);
        assertThat(archivo.contenido()).contains("UID:evento-2@motorsport-analytics", "DTEND:20250316T235959Z");
    }

    @Test
    void icsDeEvento_inexistente_lanza404() {
        when(eventoRepo.buscarConCategoria(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.icsDeEvento(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void icsDeTemporada_usaLaCarreraOElEventoCompletoSiNoTieneCarrera() {
        // Arrange
        Temporada f1 = temporada("F1");
        Evento conCarrera = evento(1L, f1, "Gran Premio de Australia", "Australia");
        Evento sinCarrera = evento(2L, f1, "Gran Premio de China", "China");
        when(temporadaRepo.buscarPorSiglasYAnio("F1", 2025)).thenReturn(Optional.of(f1));
        when(eventoRepo.listarPorTemporada(10L)).thenReturn(List.of(conCarrera, sinCarrera));
        when(sesionRepo.listarPorEventos(anyCollection())).thenReturn(List.of(
                sesion(11L, conCarrera, "Clasificación", "CLASIFICACION", "2025-03-15T05:00:00Z"),
                sesion(12L, conCarrera, "Carrera", "CARRERA", "2025-03-16T04:00:00Z")));

        // Act
        ArchivoIcs archivo = service.icsDeTemporada("F1", 2025);

        // Assert
        assertThat(archivo.nombreArchivo()).isEqualTo("f1-2025.ics");
        assertThat(cantidadDeEventos(archivo.contenido())).isEqualTo(2);
        assertThat(archivo.contenido()).contains("UID:sesion-12@motorsport-analytics", "UID:evento-2@motorsport-analytics")
                .doesNotContain("UID:sesion-11@motorsport-analytics");
    }

    @Test
    void icsDeTemporada_inexistente_lanza404() {
        when(temporadaRepo.buscarPorSiglasYAnio("F1", 1990)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.icsDeTemporada("F1", 1990)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void nombreArchivo_quitaAcentosYCaracteresEspeciales() {
        assertThat(CalendarioServiceImpl.nombreArchivo("TC", "2025", "15ª Fecha - La Plata")).isEqualTo("tc-2025-15a-fecha-la-plata.ics");
        assertThat(CalendarioServiceImpl.nombreArchivo("WRC", "2025", "Rally Sweden")).isEqualTo("wrc-2025-rally-sweden.ics");
    }
}

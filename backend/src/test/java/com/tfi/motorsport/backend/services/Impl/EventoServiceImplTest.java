package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.dtos.evento.EventoDto;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.model.Evento;
import com.tfi.motorsport.backend.model.Sesion;
import com.tfi.motorsport.backend.model.Temporada;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.repositories.EtapaJPARepository;
import com.tfi.motorsport.backend.repositories.EventoJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoEtapaJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoSesionJPARepository;
import com.tfi.motorsport.backend.repositories.SesionJPARepository;
import com.tfi.motorsport.backend.repositories.TemporadaJPARepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class EventoServiceImplTest {

    private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");

    @Mock private CategoriaJPARepository categoriaRepo;
    @Mock private TemporadaJPARepository temporadaRepo;
    @Mock private EventoJPARepository eventoRepo;
    @Mock private SesionJPARepository sesionRepo;
    @Mock private EtapaJPARepository etapaRepo;
    @Mock private ResultadoSesionJPARepository resultadoSesionRepo;
    @Mock private ResultadoEtapaJPARepository resultadoEtapaRepo;

    @InjectMocks
    private EventoServiceImpl service;

    private static Categoria categoria(Long id, String siglas) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setSiglas(siglas);
        c.setNombre(siglas);
        return c;
    }

    private static Evento evento(Long id, String nombre, String inicio) {
        Temporada t = new Temporada();
        t.setId(10L);
        t.setAnio(2025);
        t.setCategoria(categoria(1L, "F1"));
        Evento e = new Evento();
        e.setId(id);
        e.setTemporada(t);
        e.setNombre(nombre);
        e.setZonaHoraria("Australia/Melbourne");
        e.setFechaInicio(Instant.parse(inicio));
        e.setEstado("FINALIZADO");
        return e;
    }

    @Test
    void eventosDeTemporada_armaCadaEventoConSusSesionesYCantidadDeResultados() {
        // Arrange
        Evento australia = evento(1L, "Gran Premio de Australia", "2025-03-14T00:00:00Z");
        when(temporadaRepo.buscarPorSiglasYAnio("F1", 2025)).thenReturn(Optional.of(australia.getTemporada()));
        when(eventoRepo.listarPorTemporada(10L)).thenReturn(List.of(australia));
        Sesion carrera = new Sesion();
        carrera.setId(12L);
        carrera.setEvento(australia);
        carrera.setNombre("Carrera");
        carrera.setTipo("CARRERA");
        carrera.setFechaInicio(Instant.parse("2025-03-16T04:00:00Z"));
        carrera.setEstado("FINALIZADO");
        when(sesionRepo.listarPorEventos(List.of(1L))).thenReturn(List.of(carrera));
        when(sesionRepo.contarResultados(List.of(12L))).thenReturn(List.<Object[]>of(new Object[]{12L, 20L}));

        // Act
        List<EventoDto> eventos = service.eventosDeTemporada("F1", 2025, BUENOS_AIRES);

        // Assert
        assertThat(eventos).singleElement().satisfies(e -> {
            assertThat(e.categoria().siglas()).isEqualTo("F1");
            assertThat(e.sesiones()).singleElement().satisfies(s -> assertThat(s.cantidadResultados()).isEqualTo(20L));
            assertThat(e.etapas()).isEmpty();
            assertThat(e.diferenciaSedeMinutos()).as("Melbourne (UTC+11) vs Buenos Aires (UTC-3)").isEqualTo(840);
        });
    }

    @Test
    void calendario_convierteLosDiasAlRangoDeLaZonaDelUsuario() {
        // Arrange
        when(categoriaRepo.findAllByActivoAndEliminadoFalseOrderByIdAsc(true))
                .thenReturn(List.of(categoria(1L, "F1"), categoria(2L, "WRC")));
        ArgumentCaptor<Instant> desde = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> hasta = ArgumentCaptor.forClass(Instant.class);
        when(eventoRepo.calendario(desde.capture(), hasta.capture(), eq(List.of(1L, 2L)))).thenReturn(List.of());

        // Act
        List<EventoDto> eventos = service.calendario(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31), null, BUENOS_AIRES);

        // Assert: marzo en Buenos Aires (UTC-3) = 01/03 03:00 UTC hasta 01/04 02:59:59.999 UTC
        assertThat(eventos).isEmpty();
        assertThat(desde.getValue()).isEqualTo(Instant.parse("2025-03-01T03:00:00Z"));
        assertThat(hasta.getValue()).isBefore(Instant.parse("2025-04-01T03:00:00Z"))
                .isAfter(Instant.parse("2025-04-01T02:59:59Z"));
    }

    @Test
    void calendario_conCategorias_usaSoloEsasSinRepetir() {
        when(categoriaRepo.findBySiglasIgnoreCase("f1")).thenReturn(Optional.of(categoria(1L, "F1")));
        when(categoriaRepo.findBySiglasIgnoreCase("TC")).thenReturn(Optional.of(categoria(3L, "TC")));
        when(eventoRepo.calendario(any(), any(), anyCollection())).thenReturn(List.of());

        service.calendario(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31), List.of("f1", " TC ", "f1"), BUENOS_AIRES);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> ids = ArgumentCaptor.forClass(Collection.class);
        verify(eventoRepo).calendario(any(), any(), ids.capture());
        assertThat(ids.getValue()).containsExactly(1L, 3L);
    }

    @Test
    void calendario_categoriaInexistente_lanza404() {
        when(categoriaRepo.findBySiglasIgnoreCase("XX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calendario(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31),
                List.of("XX"), BUENOS_AIRES)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void calendario_hastaAnteriorADesde_lanzaErrorSinConsultar() {
        assertThatThrownBy(() -> service.calendario(LocalDate.of(2025, 3, 31), LocalDate.of(2025, 3, 1), null, BUENOS_AIRES))
                .isInstanceOf(RuleBusinessException.class);
        verifyNoInteractions(eventoRepo);
    }

    @Test
    void calendario_rangoMayorAUnAnio_lanzaError() {
        assertThatThrownBy(() -> service.calendario(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 3), null, BUENOS_AIRES))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessageContaining(String.valueOf(EventoServiceImpl.MAX_DIAS_CALENDARIO));
    }

    @Test
    void proximosYRecientes_pidenLaCantidadIndicada() {
        when(eventoRepo.proximos(any(Instant.class), any(Pageable.class))).thenReturn(List.of());
        when(eventoRepo.recientes(any(Pageable.class))).thenReturn(List.of());

        service.proximos(4, BUENOS_AIRES);
        service.recientes(6, BUENOS_AIRES);

        ArgumentCaptor<Pageable> paginaProximos = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<Pageable> paginaRecientes = ArgumentCaptor.forClass(Pageable.class);
        verify(eventoRepo).proximos(any(Instant.class), paginaProximos.capture());
        verify(eventoRepo).recientes(paginaRecientes.capture());
        assertThat(paginaProximos.getValue().getPageSize()).isEqualTo(4);
        assertThat(paginaRecientes.getValue().getPageSize()).isEqualTo(6);
    }
}

package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.dtos.gestionDatos.EventoDtos;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Catalogo.EstadoEvento;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.model.Evento;
import com.tfi.motorsport.backend.model.Temporada;
import com.tfi.motorsport.backend.model.Usuario;
import com.tfi.motorsport.backend.repositories.EventoJPARepository;
import com.tfi.motorsport.backend.repositories.TemporadaJPARepository;
import com.tfi.motorsport.backend.repositories.UsuarioJPARepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;

import static com.tfi.motorsport.backend.services.gestion.Impl.GestionDePrueba.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ABM de eventos: valores por defecto (zona UTC, fin = inicio, PROGRAMADO), zona horaria IANA válida,
 * fechas coherentes y que la temporada exista y no esté dada de baja.
 */
@ExtendWith(MockitoExtension.class)
class EventoGestionServiceImplTest {

    private static final Instant INICIO = Instant.parse("2026-03-08T04:00:00Z");

    @Mock
    private EventoJPARepository eventoRepo;

    @Mock
    private TemporadaJPARepository temporadaRepo;

    @Mock
    private UsuarioJPARepository usuarioRepo;

    private final Usuario admin = admin();
    private final Categoria f1 = categoria(1L, "F1");
    private final Temporada f1_2026 = temporada(10L, f1, 2026);
    private EventoGestionServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(usuarioRepo.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        service = new EventoGestionServiceImpl(eventoRepo, temporadaRepo, new SoporteGestion(usuarioRepo));
    }

    private static EventoDtos.Request request(Long temporadaId, String zona, Instant fin, String estado) {
        return new EventoDtos.Request(temporadaId, " Gran Premio de Australia ", "  ", 1, "Australia", "Melbourne",
                "Albert Park", zona, INICIO, fin, estado);
    }

    @Test
    void crear_sinZonaFinNiEstado_usaUtcElInicioYProgramado() {
        when(temporadaRepo.findById(10L)).thenReturn(Optional.of(f1_2026));
        when(eventoRepo.save(any(Evento.class))).thenAnswer(inv -> inv.getArgument(0));

        EventoDtos.Registro creado = service.crear(request(10L, null, null, null), ADMIN_ID);

        assertThat(creado.categoria()).isEqualTo("F1");
        assertThat(creado.anio()).isEqualTo(2026);
        assertThat(creado.nombre()).isEqualTo("Gran Premio de Australia");
        assertThat(creado.nombreCorto()).isNull();
        assertThat(creado.zonaHoraria()).isEqualTo("UTC");
        assertThat(creado.fin()).isEqualTo(INICIO);
        assertThat(creado.estado()).isEqualTo(EstadoEvento.PROGRAMADO);
        assertThat(creado.auditoria().creadoPor()).isEqualTo(ADMIN_EMAIL);
    }

    @Test
    void crear_conZonaHorariaDeLaSede_laGuarda() {
        when(temporadaRepo.findById(10L)).thenReturn(Optional.of(f1_2026));
        when(eventoRepo.save(any(Evento.class))).thenAnswer(inv -> inv.getArgument(0));

        EventoDtos.Registro creado = service.crear(
                request(10L, " Australia/Melbourne ", INICIO.plusSeconds(7200), EstadoEvento.FINALIZADO), ADMIN_ID);

        assertThat(creado.zonaHoraria()).isEqualTo("Australia/Melbourne");
        assertThat(creado.fin()).isEqualTo(INICIO.plusSeconds(7200));
        assertThat(creado.estado()).isEqualTo(EstadoEvento.FINALIZADO);
    }

    @Test
    void crear_zonaHorariaInvalida_lanza400SinGuardar() {
        when(temporadaRepo.findById(10L)).thenReturn(Optional.of(f1_2026));

        assertThatThrownBy(() -> service.crear(request(10L, "Marte/Olympus", null, null), ADMIN_ID))
                .isInstanceOfSatisfying(RuleBusinessException.class, conEstado(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("Zona horaria inválida");
        verify(eventoRepo, never()).save(any());
    }

    @Test
    void crear_finAnteriorAlInicio_lanza400SinGuardar() {
        when(temporadaRepo.findById(10L)).thenReturn(Optional.of(f1_2026));

        assertThatThrownBy(() -> service.crear(request(10L, null, INICIO.minusSeconds(60), null), ADMIN_ID))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessage("La fecha y hora de fin no puede ser anterior a la de inicio");
        verify(eventoRepo, never()).save(any());
    }

    @Test
    void crear_enUnaTemporadaDadaDeBaja_lanza404() {
        when(temporadaRepo.findById(10L)).thenReturn(Optional.of(dadoDeBaja(temporada(10L, f1, 2026))));

        assertThatThrownBy(() -> service.crear(request(10L, null, null, null), ADMIN_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(eventoRepo);
    }

    @Test
    void modificar_aOtraTemporada_validaLaNuevaYRegistraQuienLoCambio() {
        Evento evento = evento(20L, temporada(9L, f1, 2025));
        when(eventoRepo.findById(20L)).thenReturn(Optional.of(evento));
        when(temporadaRepo.findById(10L)).thenReturn(Optional.of(f1_2026));

        EventoDtos.Registro modificado = service.modificar(20L, request(10L, null, null, null), ADMIN_ID);

        assertThat(modificado.temporadaId()).isEqualTo(10L);
        assertThat(evento.getTemporada()).isSameAs(f1_2026);
        assertThat(evento.getActualizadoPor()).isSameAs(admin);
    }

    @Test
    void eliminar_esBajaLogica() {
        Evento evento = evento(20L, f1_2026);
        when(eventoRepo.findById(20L)).thenReturn(Optional.of(evento));

        service.eliminar(20L, ADMIN_ID);

        assertThat(evento.isEliminado()).isTrue();
        assertThat(evento.getActualizadoPor()).isSameAs(admin);
        verify(eventoRepo, never()).delete(any());
    }
}

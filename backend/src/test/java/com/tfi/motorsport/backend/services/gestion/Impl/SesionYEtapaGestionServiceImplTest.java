package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.dtos.gestionDatos.EtapaDtos;
import com.tfi.motorsport.backend.dtos.gestionDatos.SesionDtos;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Catalogo;
import com.tfi.motorsport.backend.model.Catalogo.EstadoEvento;
import com.tfi.motorsport.backend.model.Catalogo.TipoSesion;
import com.tfi.motorsport.backend.model.Etapa;
import com.tfi.motorsport.backend.model.Evento;
import com.tfi.motorsport.backend.model.Sesion;
import com.tfi.motorsport.backend.model.Usuario;
import com.tfi.motorsport.backend.repositories.EtapaJPARepository;
import com.tfi.motorsport.backend.repositories.EventoJPARepository;
import com.tfi.motorsport.backend.repositories.SesionJPARepository;
import com.tfi.motorsport.backend.repositories.UsuarioJPARepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static com.tfi.motorsport.backend.services.gestion.Impl.GestionDePrueba.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ABM de sesiones (F1, TC) y etapas (WRC): estado por defecto, evento activo y una sola clasificación
 * general por rally (si estaba dada de baja se reactiva).
 */
@ExtendWith(MockitoExtension.class)
class SesionYEtapaGestionServiceImplTest {

    private static final Instant INICIO = Instant.parse("2026-03-08T04:00:00Z");

    @Mock
    private SesionJPARepository sesionRepo;

    @Mock
    private EtapaJPARepository etapaRepo;

    @Mock
    private EventoJPARepository eventoRepo;

    @Mock
    private UsuarioJPARepository usuarioRepo;

    private final Usuario admin = admin();
    private final Evento australia = evento(20L, temporada(10L, categoria(1L, "F1"), 2026));
    private final Evento monteCarlo = evento(30L, temporada(11L, categoria(2L, "WRC"), 2026));
    private SesionGestionServiceImpl sesiones;
    private EtapaGestionServiceImpl etapas;

    @BeforeEach
    void setUp() {
        lenient().when(usuarioRepo.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        SoporteGestion soporte = new SoporteGestion(usuarioRepo);
        sesiones = new SesionGestionServiceImpl(sesionRepo, eventoRepo, soporte);
        etapas = new EtapaGestionServiceImpl(etapaRepo, eventoRepo, soporte);
    }

    private static SesionDtos.Request requestSesion(Long eventoId, String estado) {
        return new SesionDtos.Request(eventoId, " Carrera ", TipoSesion.CARRERA, 1, INICIO, INICIO.plusSeconds(7200),
                58, new BigDecimal("306.124"), estado);
    }

    private static EtapaDtos.Request requestEtapa(Boolean clasificacionGeneral) {
        return new EtapaDtos.Request(30L, 1, " SS1 Col de Turini ", 1, new BigDecimal("15.10"), INICIO, null, null,
                clasificacionGeneral);
    }

    // ---------------------------------------------------------------- sesiones

    @Test
    void crearSesion_sinEstado_quedaProgramadaYConAuditoria() {
        when(eventoRepo.findById(20L)).thenReturn(Optional.of(australia));
        when(sesionRepo.save(any(Sesion.class))).thenAnswer(inv -> inv.getArgument(0));

        SesionDtos.Registro creada = sesiones.crear(requestSesion(20L, null), ADMIN_ID);

        assertThat(creada.eventoId()).isEqualTo(20L);
        assertThat(creada.nombre()).isEqualTo("Carrera");
        assertThat(creada.tipo()).isEqualTo(TipoSesion.CARRERA);
        assertThat(creada.estado()).isEqualTo(EstadoEvento.PROGRAMADO);
        assertThat(creada.auditoria().creadoPor()).isEqualTo(ADMIN_EMAIL);
    }

    @Test
    void crearSesion_enUnEventoDadoDeBaja_lanza404() {
        when(eventoRepo.findById(20L)).thenReturn(Optional.of(dadoDeBaja(evento(20L, australia.getTemporada()))));

        assertThatThrownBy(() -> sesiones.crear(requestSesion(20L, null), ADMIN_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(sesionRepo);
    }

    @Test
    void modificarSesion_conservaElEventoYActualizaLaAuditoria() {
        Sesion carrera = sesion(40L, australia);
        when(sesionRepo.findById(40L)).thenReturn(Optional.of(carrera));

        SesionDtos.Registro modificada = sesiones.modificar(40L, requestSesion(20L, EstadoEvento.FINALIZADO), ADMIN_ID);

        assertThat(modificada.estado()).isEqualTo(EstadoEvento.FINALIZADO);
        assertThat(carrera.getVueltasProgramadas()).isEqualTo(58);
        assertThat(carrera.getActualizadoPor()).isSameAs(admin);
        verifyNoInteractions(eventoRepo);
    }

    @Test
    void eliminarSesion_esBajaLogica() {
        Sesion carrera = sesion(40L, australia);
        when(sesionRepo.findById(40L)).thenReturn(Optional.of(carrera));

        sesiones.eliminar(40L, ADMIN_ID);

        assertThat(carrera.isEliminado()).isTrue();
        assertThat(carrera.getActualizadoPor()).isSameAs(admin);
    }

    // ---------------------------------------------------------------- etapas

    @Test
    void crearEtapa_comun_noEsClasificacionGeneral() {
        when(eventoRepo.findById(30L)).thenReturn(Optional.of(monteCarlo));
        when(etapaRepo.save(any(Etapa.class))).thenAnswer(inv -> inv.getArgument(0));

        EtapaDtos.Registro creada = etapas.crear(requestEtapa(false), ADMIN_ID);

        assertThat(creada.nombre()).isEqualTo("SS1 Col de Turini");
        assertThat(creada.idExterno()).isNull();
        assertThat(creada.clasificacionGeneral()).isFalse();
        assertThat(creada.estado()).isEqualTo(EstadoEvento.PROGRAMADO);
        verify(etapaRepo, never()).findByEventoIdAndIdExterno(any(), any());
    }

    @Test
    void crearEtapa_clasificacionGeneral_seMarcaConElIdExternoGeneral() {
        when(eventoRepo.findById(30L)).thenReturn(Optional.of(monteCarlo));
        when(etapaRepo.findByEventoIdAndIdExterno(30L, Catalogo.ETAPA_GENERAL)).thenReturn(Optional.empty());
        when(etapaRepo.save(any(Etapa.class))).thenAnswer(inv -> inv.getArgument(0));

        EtapaDtos.Registro creada = etapas.crear(requestEtapa(true), ADMIN_ID);

        assertThat(creada.idExterno()).isEqualTo(Catalogo.ETAPA_GENERAL);
        assertThat(creada.clasificacionGeneral()).isTrue();
    }

    @Test
    void crearEtapa_segundaClasificacionGeneral_lanza409() {
        when(eventoRepo.findById(30L)).thenReturn(Optional.of(monteCarlo));
        Etapa general = etapa(50L, monteCarlo);
        general.setIdExterno(Catalogo.ETAPA_GENERAL);
        when(etapaRepo.findByEventoIdAndIdExterno(30L, Catalogo.ETAPA_GENERAL)).thenReturn(Optional.of(general));

        assertThatThrownBy(() -> etapas.crear(requestEtapa(true), ADMIN_ID))
                .isInstanceOfSatisfying(RuleBusinessException.class, conEstado(HttpStatus.CONFLICT))
                .hasMessage("El evento ya tiene una clasificación general");
        verify(etapaRepo, never()).save(any());
    }

    @Test
    void crearEtapa_clasificacionGeneralDadaDeBaja_seReactiva() {
        when(eventoRepo.findById(30L)).thenReturn(Optional.of(monteCarlo));
        Etapa general = dadoDeBaja(etapa(50L, monteCarlo));
        general.setIdExterno(Catalogo.ETAPA_GENERAL);
        when(etapaRepo.findByEventoIdAndIdExterno(30L, Catalogo.ETAPA_GENERAL)).thenReturn(Optional.of(general));
        when(etapaRepo.save(general)).thenReturn(general);

        EtapaDtos.Registro reactivada = etapas.crear(requestEtapa(true), ADMIN_ID);

        assertThat(reactivada.id()).isEqualTo(50L);
        assertThat(general.isEliminado()).isFalse();
        assertThat(general.getNombre()).isEqualTo("SS1 Col de Turini");
    }
}

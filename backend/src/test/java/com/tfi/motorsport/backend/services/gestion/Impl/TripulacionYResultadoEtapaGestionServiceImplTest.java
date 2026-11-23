package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.dtos.gestionDatos.ResultadoEtapaDtos;
import com.tfi.motorsport.backend.dtos.gestionDatos.TripulacionDtos;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Catalogo;
import com.tfi.motorsport.backend.model.Catalogo.EstadoResultado;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.model.Equipo;
import com.tfi.motorsport.backend.model.Etapa;
import com.tfi.motorsport.backend.model.Piloto;
import com.tfi.motorsport.backend.model.ResultadoEtapa;
import com.tfi.motorsport.backend.model.Tripulacion;
import com.tfi.motorsport.backend.model.Usuario;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.repositories.EquipoJPARepository;
import com.tfi.motorsport.backend.repositories.EtapaJPARepository;
import com.tfi.motorsport.backend.repositories.PilotoJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoEtapaJPARepository;
import com.tfi.motorsport.backend.repositories.TripulacionJPARepository;
import com.tfi.motorsport.backend.repositories.UsuarioJPARepository;
import com.tfi.motorsport.backend.services.gestion.IParticipacionGestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static com.tfi.motorsport.backend.services.gestion.Impl.GestionDePrueba.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * WRC: tripulaciones (piloto y copiloto distintos, una por temporada, no se dan de baja con resultados)
 * y resultados de etapa (la tripulación tiene que ser de la categoría y temporada del rally).
 */
@ExtendWith(MockitoExtension.class)
class TripulacionYResultadoEtapaGestionServiceImplTest {

    @Mock
    private TripulacionJPARepository tripulacionRepo;

    @Mock
    private ResultadoEtapaJPARepository resultadoEtapaRepo;

    @Mock
    private EtapaJPARepository etapaRepo;

    @Mock
    private CategoriaJPARepository categoriaRepo;

    @Mock
    private PilotoJPARepository pilotoRepo;

    @Mock
    private EquipoJPARepository equipoRepo;

    @Mock
    private IParticipacionGestionService participaciones;

    @Mock
    private UsuarioJPARepository usuarioRepo;

    private final Usuario admin = admin();
    private final Categoria wrc = categoria(2L, "WRC");
    private final Etapa ss1 = etapa(50L, evento(30L, temporada(11L, wrc, 2026)));
    private final Piloto ogier = piloto(1L, "Sébastien", "Ogier");
    private final Piloto landais = piloto(2L, "Vincent", "Landais");
    private final Equipo toyota = equipo(5L, "Toyota Gazoo Racing WRT");
    private TripulacionGestionServiceImpl tripulaciones;
    private ResultadoEtapaGestionServiceImpl resultados;

    @BeforeEach
    void setUp() {
        lenient().when(usuarioRepo.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        SoporteGestion soporte = new SoporteGestion(usuarioRepo);
        tripulaciones = new TripulacionGestionServiceImpl(tripulacionRepo, categoriaRepo, pilotoRepo, equipoRepo,
                resultadoEtapaRepo, participaciones, soporte);
        resultados = new ResultadoEtapaGestionServiceImpl(resultadoEtapaRepo, etapaRepo, tripulacionRepo, soporte);
    }

    private static TripulacionDtos.Request requestTripulacion(Long pilotoId, Long copilotoId) {
        return new TripulacionDtos.Request(2L, 2026, pilotoId, copilotoId, 5L, 17);
    }

    private static ResultadoEtapaDtos.Request requestResultado(Long tripulacionId) {
        return new ResultadoEtapaDtos.Request(50L, tripulacionId, 1, new BigDecimal("812.4"), null, null, null, null);
    }

    private Tripulacion ogierLandais(int temporada) {
        return tripulacion(60L, wrc, temporada, ogier, landais, toyota);
    }

    private ResultadoEtapa resultadoDe(Tripulacion tripulacion) {
        ResultadoEtapa r = new ResultadoEtapa();
        r.setId(80L);
        r.setEtapa(ss1);
        r.setTripulacion(tripulacion);
        r.setPosicion(1);
        return r;
    }

    /** Categoría, piloto, copiloto y equipo existen y están activos. */
    private void referenciasActivas() {
        when(categoriaRepo.findById(2L)).thenReturn(Optional.of(wrc));
        when(pilotoRepo.findById(1L)).thenReturn(Optional.of(ogier));
        when(pilotoRepo.findById(2L)).thenReturn(Optional.of(landais));
        when(equipoRepo.findById(5L)).thenReturn(Optional.of(toyota));
    }

    // ---------------------------------------------------------------- tripulaciones

    @Test
    void crearTripulacion_pilotoYCopilotoIguales_lanza400SinConsultarNada() {
        assertThatThrownBy(() -> tripulaciones.crear(requestTripulacion(1L, 1L), ADMIN_ID))
                .isInstanceOfSatisfying(RuleBusinessException.class, conEstado(HttpStatus.BAD_REQUEST))
                .hasMessage("El piloto y el copiloto deben ser personas distintas");
        verifyNoInteractions(tripulacionRepo, categoriaRepo, pilotoRepo, equipoRepo, participaciones);
    }

    @Test
    void crearTripulacion_nueva_registraLasParticipacionesDePilotoYCopiloto() {
        referenciasActivas();
        when(tripulacionRepo.findByCategoriaIdAndTemporadaAndPilotoIdAndCopilotoIdAndEquipoId(2L, 2026, 1L, 2L, 5L))
                .thenReturn(Optional.empty());
        when(categoriaRepo.getReferenceById(2L)).thenReturn(wrc);
        when(pilotoRepo.getReferenceById(1L)).thenReturn(ogier);
        when(pilotoRepo.getReferenceById(2L)).thenReturn(landais);
        when(equipoRepo.getReferenceById(5L)).thenReturn(toyota);
        when(tripulacionRepo.save(any(Tripulacion.class))).thenAnswer(inv -> inv.getArgument(0));

        TripulacionDtos.Registro creada = tripulaciones.crear(requestTripulacion(1L, 2L), ADMIN_ID);

        assertThat(creada.piloto()).isEqualTo("Sébastien Ogier");
        assertThat(creada.copiloto()).isEqualTo("Vincent Landais");
        assertThat(creada.equipo()).isEqualTo("Toyota Gazoo Racing WRT");
        assertThat(creada.auditoria().creadoPor()).isEqualTo(ADMIN_EMAIL);
        verify(participaciones).registrarSiNoExiste(5L, 1L, 2L, 2026, 17, Catalogo.ROL_PILOTO, ADMIN_ID);
        verify(participaciones).registrarSiNoExiste(5L, 2L, 2L, 2026, 17, Catalogo.ROL_COPILOTO, ADMIN_ID);
    }

    @Test
    void crearTripulacion_repetidaEnLaTemporada_lanza409() {
        referenciasActivas();
        when(tripulacionRepo.findByCategoriaIdAndTemporadaAndPilotoIdAndCopilotoIdAndEquipoId(2L, 2026, 1L, 2L, 5L))
                .thenReturn(Optional.of(ogierLandais(2026)));

        assertThatThrownBy(() -> tripulaciones.crear(requestTripulacion(1L, 2L), ADMIN_ID))
                .isInstanceOfSatisfying(RuleBusinessException.class, conEstado(HttpStatus.CONFLICT))
                .hasMessage("Esa tripulación ya existe para la temporada 2026");
        verify(tripulacionRepo, never()).save(any());
        verifyNoInteractions(participaciones);
    }

    @Test
    void crearTripulacion_conUnCopilotoDadoDeBaja_lanza404() {
        when(categoriaRepo.findById(2L)).thenReturn(Optional.of(wrc));
        when(pilotoRepo.findById(1L)).thenReturn(Optional.of(ogier));
        when(pilotoRepo.findById(2L)).thenReturn(Optional.of(dadoDeBaja(piloto(2L, "Vincent", "Landais"))));

        assertThatThrownBy(() -> tripulaciones.crear(requestTripulacion(1L, 2L), ADMIN_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No se encontró el copiloto con ID 2");
    }

    @Test
    void eliminarTripulacion_conResultadosDeEtapa_lanza409YNoLaDaDeBaja() {
        Tripulacion tripulacion = ogierLandais(2026);
        when(tripulacionRepo.findById(60L)).thenReturn(Optional.of(tripulacion));
        when(resultadoEtapaRepo.countByTripulacionIdAndEliminadoFalse(60L)).thenReturn(4L);

        assertThatThrownBy(() -> tripulaciones.eliminar(60L, ADMIN_ID))
                .isInstanceOfSatisfying(RuleBusinessException.class, conEstado(HttpStatus.CONFLICT))
                .hasMessageContaining("4 resultados de etapa");
        assertThat(tripulacion.isEliminado()).isFalse();
    }

    @Test
    void eliminarTripulacion_sinResultados_esBajaLogica() {
        Tripulacion tripulacion = ogierLandais(2026);
        when(tripulacionRepo.findById(60L)).thenReturn(Optional.of(tripulacion));

        tripulaciones.eliminar(60L, ADMIN_ID);

        assertThat(tripulacion.isEliminado()).isTrue();
        assertThat(tripulacion.getActualizadoPor()).isSameAs(admin);
    }

    // ---------------------------------------------------------------- resultados de etapa

    @Test
    void crearResultadoEtapa_deUnaTripulacionDelRally_seGuarda() {
        when(etapaRepo.findById(50L)).thenReturn(Optional.of(ss1));
        when(tripulacionRepo.findById(60L)).thenReturn(Optional.of(ogierLandais(2026)));
        when(resultadoEtapaRepo.findByEtapaIdAndTripulacionId(50L, 60L)).thenReturn(Optional.empty());
        when(resultadoEtapaRepo.save(any(ResultadoEtapa.class))).thenAnswer(inv -> inv.getArgument(0));

        ResultadoEtapaDtos.Registro creado = resultados.crear(requestResultado(60L), ADMIN_ID);

        assertThat(creado.tripulacion()).isEqualTo("Sébastien Ogier / Vincent Landais");
        assertThat(creado.posicion()).isEqualTo(1);
        assertThat(creado.estado()).isEqualTo(EstadoResultado.FINALIZADO);
        assertThat(creado.auditoria().creadoPor()).isEqualTo(ADMIN_EMAIL);
    }

    @Test
    void crearResultadoEtapa_tripulacionDeOtraTemporada_lanza400() {
        when(etapaRepo.findById(50L)).thenReturn(Optional.of(ss1));
        when(tripulacionRepo.findById(60L)).thenReturn(Optional.of(ogierLandais(2025)));

        assertThatThrownBy(() -> resultados.crear(requestResultado(60L), ADMIN_ID))
                .isInstanceOfSatisfying(RuleBusinessException.class, conEstado(HttpStatus.BAD_REQUEST))
                .hasMessage("La tripulación no corresponde a WRC 2026");
        verifyNoInteractions(resultadoEtapaRepo);
    }

    @Test
    void crearResultadoEtapa_tripulacionDeOtraCategoria_lanza400() {
        when(etapaRepo.findById(50L)).thenReturn(Optional.of(ss1));
        when(tripulacionRepo.findById(60L))
                .thenReturn(Optional.of(tripulacion(60L, categoria(9L, "ERC"), 2026, ogier, landais, toyota)));

        assertThatThrownBy(() -> resultados.crear(requestResultado(60L), ADMIN_ID))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessage("La tripulación no corresponde a WRC 2026");
        verifyNoInteractions(resultadoEtapaRepo);
    }

    @Test
    void crearResultadoEtapa_repetido_lanza409() {
        Tripulacion tripulacion = ogierLandais(2026);
        when(etapaRepo.findById(50L)).thenReturn(Optional.of(ss1));
        when(tripulacionRepo.findById(60L)).thenReturn(Optional.of(tripulacion));
        when(resultadoEtapaRepo.findByEtapaIdAndTripulacionId(50L, 60L)).thenReturn(Optional.of(resultadoDe(tripulacion)));

        assertThatThrownBy(() -> resultados.crear(requestResultado(60L), ADMIN_ID))
                .isInstanceOfSatisfying(RuleBusinessException.class, conEstado(HttpStatus.CONFLICT))
                .hasMessage("La tripulación ya tiene un resultado en esta etapa: modifíquelo");
        verify(resultadoEtapaRepo, never()).save(any());
    }

    @Test
    void modificarResultadoEtapa_cambiarLaTripulacion_lanza400() {
        when(resultadoEtapaRepo.findById(80L)).thenReturn(Optional.of(resultadoDe(ogierLandais(2026))));

        assertThatThrownBy(() -> resultados.modificar(80L, requestResultado(61L), ADMIN_ID))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessage("No se puede cambiar la etapa ni la tripulación de un resultado: elimínelo y cree otro");
    }

    @Test
    void eliminarResultadoEtapa_esBajaLogica() {
        ResultadoEtapa resultado = resultadoDe(ogierLandais(2026));
        when(resultadoEtapaRepo.findById(80L)).thenReturn(Optional.of(resultado));

        resultados.eliminar(80L, ADMIN_ID);

        assertThat(resultado.isEliminado()).isTrue();
        assertThat(resultado.getActualizadoPor()).isSameAs(admin);
    }
}

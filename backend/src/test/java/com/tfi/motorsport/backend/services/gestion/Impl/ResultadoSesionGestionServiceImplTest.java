package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.dtos.gestionDatos.ResultadoSesionDtos;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Catalogo;
import com.tfi.motorsport.backend.model.Catalogo.EstadoResultado;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.model.Equipo;
import com.tfi.motorsport.backend.model.Piloto;
import com.tfi.motorsport.backend.model.ResultadoSesion;
import com.tfi.motorsport.backend.model.Sesion;
import com.tfi.motorsport.backend.model.Usuario;
import com.tfi.motorsport.backend.repositories.EquipoJPARepository;
import com.tfi.motorsport.backend.repositories.PilotoJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoSesionJPARepository;
import com.tfi.motorsport.backend.repositories.SesionJPARepository;
import com.tfi.motorsport.backend.repositories.UsuarioJPARepository;
import com.tfi.motorsport.backend.services.gestion.IParticipacionGestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.tfi.motorsport.backend.services.gestion.Impl.GestionDePrueba.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Resultados de sesión (F1, TC): un resultado por piloto y sesión (si estaba dado de baja se reactiva),
 * posiciones sin repetir, sesión y piloto fijos al modificar, y la participación del piloto en la
 * temporada que se registra sola.
 */
@ExtendWith(MockitoExtension.class)
class ResultadoSesionGestionServiceImplTest {

    @Mock
    private ResultadoSesionJPARepository resultadoSesionRepo;

    @Mock
    private SesionJPARepository sesionRepo;

    @Mock
    private PilotoJPARepository pilotoRepo;

    @Mock
    private EquipoJPARepository equipoRepo;

    @Mock
    private IParticipacionGestionService participaciones;

    @Mock
    private UsuarioJPARepository usuarioRepo;

    private final Usuario admin = admin();
    private final Categoria tc = categoria(3L, "TC");
    private final Sesion finalTc = sesion(40L, evento(20L, temporada(10L, tc, 2026)));
    private final Piloto canapino = piloto(1L, "Agustín", "Canapino");
    private final Piloto rossi = piloto(2L, "Matías", "Rossi");
    private final Equipo canning = equipo(5L, "Canning Motorsport");
    private ResultadoSesionGestionServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(usuarioRepo.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        service = new ResultadoSesionGestionServiceImpl(resultadoSesionRepo, sesionRepo, pilotoRepo, equipoRepo,
                participaciones, new SoporteGestion(usuarioRepo));
    }

    private static ResultadoSesionDtos.Request request(Long pilotoId, Integer posicion, String estado) {
        return new ResultadoSesionDtos.Request(40L, pilotoId, 5L, posicion, 12, new BigDecimal("40"), null, null,
                25, null, estado, null);
    }

    private ResultadoSesion resultado(Long id, Piloto piloto, Integer posicion) {
        ResultadoSesion r = new ResultadoSesion();
        r.setId(id);
        r.setSesion(finalTc);
        r.setPiloto(piloto);
        r.setEquipo(canning);
        r.setPosicion(posicion);
        return r;
    }

    /** Sesión, piloto y equipo existen y están activos. */
    private void referenciasActivas() {
        when(sesionRepo.findById(40L)).thenReturn(Optional.of(finalTc));
        when(pilotoRepo.findById(1L)).thenReturn(Optional.of(canapino));
        when(equipoRepo.findById(5L)).thenReturn(Optional.of(canning));
    }

    @Test
    void crear_guardaElResultadoYRegistraLaParticipacionDelPilotoEnLaTemporada() {
        referenciasActivas();
        when(resultadoSesionRepo.findBySesionIdAndPilotoId(40L, 1L)).thenReturn(Optional.empty());
        when(resultadoSesionRepo.tablaDeSesion(40L)).thenReturn(List.of());
        when(pilotoRepo.getReferenceById(1L)).thenReturn(canapino);
        when(equipoRepo.getReferenceById(5L)).thenReturn(canning);
        when(resultadoSesionRepo.save(any(ResultadoSesion.class))).thenAnswer(inv -> inv.getArgument(0));

        ResultadoSesionDtos.Registro creado = service.crear(request(1L, 1, null), ADMIN_ID);

        assertThat(creado.piloto()).isEqualTo("Agustín Canapino");
        assertThat(creado.equipo()).isEqualTo("Canning Motorsport");
        assertThat(creado.posicion()).isEqualTo(1);
        assertThat(creado.estado()).isEqualTo(EstadoResultado.FINALIZADO);
        assertThat(creado.auditoria().creadoPor()).isEqualTo(ADMIN_EMAIL);
        verify(participaciones).registrarSiNoExiste(5L, 1L, 3L, 2026, 12, Catalogo.ROL_PILOTO, ADMIN_ID);
    }

    @Test
    void crear_sinPosicion_noRevisaLaTablaDeLaSesion() {
        referenciasActivas();
        when(resultadoSesionRepo.findBySesionIdAndPilotoId(40L, 1L)).thenReturn(Optional.empty());
        when(pilotoRepo.getReferenceById(1L)).thenReturn(canapino);
        when(equipoRepo.getReferenceById(5L)).thenReturn(canning);
        when(resultadoSesionRepo.save(any(ResultadoSesion.class))).thenAnswer(inv -> inv.getArgument(0));

        ResultadoSesionDtos.Registro creado = service.crear(request(1L, null, EstadoResultado.ABANDONO), ADMIN_ID);

        assertThat(creado.posicion()).isNull();
        assertThat(creado.estado()).isEqualTo(EstadoResultado.ABANDONO);
        verify(resultadoSesionRepo, never()).tablaDeSesion(any());
    }

    @Test
    void crear_pilotoQueYaTieneResultadoEnLaSesion_lanza409() {
        referenciasActivas();
        when(resultadoSesionRepo.findBySesionIdAndPilotoId(40L, 1L))
                .thenReturn(Optional.of(resultado(70L, canapino, 3)));

        assertThatThrownBy(() -> service.crear(request(1L, 1, null), ADMIN_ID))
                .isInstanceOfSatisfying(RuleBusinessException.class, conEstado(HttpStatus.CONFLICT))
                .hasMessage("El piloto ya tiene un resultado en esta sesión: modifíquelo");
        verify(resultadoSesionRepo, never()).save(any());
        verifyNoInteractions(participaciones);
    }

    @Test
    void crear_posicionOcupadaPorOtroPiloto_lanza409() {
        referenciasActivas();
        when(resultadoSesionRepo.findBySesionIdAndPilotoId(40L, 1L)).thenReturn(Optional.empty());
        when(resultadoSesionRepo.tablaDeSesion(40L)).thenReturn(List.of(resultado(71L, rossi, 1)));

        assertThatThrownBy(() -> service.crear(request(1L, 1, null), ADMIN_ID))
                .isInstanceOfSatisfying(RuleBusinessException.class, conEstado(HttpStatus.CONFLICT))
                .hasMessage("La posición 1 ya está asignada a Matías Rossi");
        verify(resultadoSesionRepo, never()).save(any());
    }

    @Test
    void crear_resultadoDadoDeBaja_seReactivaConLosDatosNuevos() {
        ResultadoSesion viejo = dadoDeBaja(resultado(70L, canapino, 3));
        referenciasActivas();
        when(resultadoSesionRepo.findBySesionIdAndPilotoId(40L, 1L)).thenReturn(Optional.of(viejo));
        when(resultadoSesionRepo.tablaDeSesion(40L)).thenReturn(List.of());
        when(equipoRepo.getReferenceById(5L)).thenReturn(canning);
        when(resultadoSesionRepo.save(viejo)).thenReturn(viejo);

        ResultadoSesionDtos.Registro reactivado = service.crear(request(1L, 1, null), ADMIN_ID);

        assertThat(reactivado.id()).isEqualTo(70L);
        assertThat(viejo.isEliminado()).isFalse();
        assertThat(viejo.getPosicion()).isEqualTo(1);
    }

    @Test
    void modificar_cambiarElPiloto_lanza400() {
        when(resultadoSesionRepo.findById(70L)).thenReturn(Optional.of(resultado(70L, canapino, 3)));

        assertThatThrownBy(() -> service.modificar(70L, request(2L, 3, null), ADMIN_ID))
                .isInstanceOfSatisfying(RuleBusinessException.class, conEstado(HttpStatus.BAD_REQUEST))
                .hasMessage("No se puede cambiar la sesión ni el piloto de un resultado: elimínelo y cree otro");
    }

    @Test
    void modificar_conservandoSuPropiaPosicion_esValido() {
        ResultadoSesion resultado = resultado(70L, canapino, 3);
        when(resultadoSesionRepo.findById(70L)).thenReturn(Optional.of(resultado));
        when(equipoRepo.findById(5L)).thenReturn(Optional.of(canning));
        when(resultadoSesionRepo.tablaDeSesion(40L)).thenReturn(List.of(resultado, resultado(71L, rossi, 1)));
        when(equipoRepo.getReferenceById(5L)).thenReturn(canning);

        ResultadoSesionDtos.Registro modificado = service.modificar(70L, request(1L, 3, null), ADMIN_ID);

        assertThat(modificado.posicion()).isEqualTo(3);
        assertThat(modificado.puntos()).isEqualByComparingTo("40");
        assertThat(resultado.getActualizadoPor()).isSameAs(admin);
    }

    @Test
    void eliminar_esBajaLogica() {
        ResultadoSesion resultado = resultado(70L, canapino, 3);
        when(resultadoSesionRepo.findById(70L)).thenReturn(Optional.of(resultado));

        service.eliminar(70L, ADMIN_ID);

        assertThat(resultado.isEliminado()).isTrue();
        assertThat(resultado.getActualizadoPor()).isSameAs(admin);
    }
}

package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.dtos.gestionDatos.EquipoDtos;
import com.tfi.motorsport.backend.dtos.gestionDatos.PilotoDtos;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Equipo;
import com.tfi.motorsport.backend.model.Piloto;
import com.tfi.motorsport.backend.model.Usuario;
import com.tfi.motorsport.backend.repositories.EquipoJPARepository;
import com.tfi.motorsport.backend.repositories.PilotoJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoSesionJPARepository;
import com.tfi.motorsport.backend.repositories.TripulacionJPARepository;
import com.tfi.motorsport.backend.repositories.UsuarioJPARepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.tfi.motorsport.backend.services.gestion.Impl.GestionDePrueba.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ABM de pilotos y equipos: normalización de textos, tope del listado y la regla de integridad de la baja
 * (no se da de baja a quien todavía tiene resultados o tripulaciones activas).
 */
@ExtendWith(MockitoExtension.class)
class PilotoYEquipoGestionServiceImplTest {

    @Mock
    private PilotoJPARepository pilotoRepo;

    @Mock
    private EquipoJPARepository equipoRepo;

    @Mock
    private ResultadoSesionJPARepository resultadoSesionRepo;

    @Mock
    private TripulacionJPARepository tripulacionRepo;

    @Mock
    private UsuarioJPARepository usuarioRepo;

    private final Usuario admin = admin();
    private PilotoGestionServiceImpl pilotos;
    private EquipoGestionServiceImpl equipos;

    @BeforeEach
    void setUp() {
        lenient().when(usuarioRepo.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        SoporteGestion soporte = new SoporteGestion(usuarioRepo);
        pilotos = new PilotoGestionServiceImpl(pilotoRepo, resultadoSesionRepo, tripulacionRepo, soporte);
        equipos = new EquipoGestionServiceImpl(equipoRepo, resultadoSesionRepo, tripulacionRepo, soporte);
    }

    private static PilotoDtos.Request requestPiloto(LocalDate inicioCarrera, LocalDate finCarrera) {
        return new PilotoDtos.Request(" Franco ", " Colapinto ", LocalDate.of(2003, 5, 27), " argentina ", "  ",
                "col", 43, "", inicioCarrera, finCarrera);
    }

    private static EquipoDtos.Request requestEquipo() {
        return new EquipoDtos.Request(" Canning Motorsport ", " Ford ", "  ", null, null, " Salto ");
    }

    // ---------------------------------------------------------------- pilotos

    @Test
    void crearPiloto_normalizaLosTextosYRegistraLaAuditoria() {
        when(pilotoRepo.save(any(Piloto.class))).thenAnswer(inv -> inv.getArgument(0));

        PilotoDtos.Registro creado = pilotos.crear(requestPiloto(LocalDate.of(2019, 1, 1), null), ADMIN_ID);

        assertThat(creado.nombre()).isEqualTo("Franco");
        assertThat(creado.apellido()).isEqualTo("Colapinto");
        assertThat(creado.nacionalidad()).isEqualTo("argentina");
        assertThat(creado.pais()).isNull();
        assertThat(creado.codigo()).isEqualTo("COL");
        assertThat(creado.numero()).isEqualTo(43);
        assertThat(creado.fotoUrl()).isNull();
        assertThat(creado.auditoria().creadoPor()).isEqualTo(ADMIN_EMAIL);
    }

    @Test
    void crearPiloto_finDeCarreraAnteriorAlInicio_lanza400SinGuardar() {
        assertThatThrownBy(() -> pilotos.crear(requestPiloto(LocalDate.of(2020, 1, 1), LocalDate.of(2019, 1, 1)), ADMIN_ID))
                .isInstanceOfSatisfying(RuleBusinessException.class, conEstado(HttpStatus.BAD_REQUEST));
        verify(pilotoRepo, never()).save(any());
    }

    @Test
    void listarPilotos_limiteMayorAlTope_seRecortaA500() {
        when(pilotoRepo.buscar(eq("%colapinto%"), eq(false), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(piloto(1L, "Franco", "Colapinto"))));
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);

        List<PilotoDtos.Registro> encontrados = pilotos.listar(" Colapinto ", 5000);

        assertThat(encontrados).extracting(PilotoDtos.Registro::apellido).containsExactly("Colapinto");
        verify(pilotoRepo).buscar(eq("%colapinto%"), eq(false), isNull(), isNull(), pagina.capture());
        assertThat(pagina.getValue().getPageSize()).isEqualTo(SoporteGestion.LIMITE_LISTADO);
    }

    @Test
    void eliminarPiloto_conResultadosActivos_lanza409YNoLoDaDeBaja() {
        Piloto colapinto = piloto(1L, "Franco", "Colapinto");
        when(pilotoRepo.findById(1L)).thenReturn(Optional.of(colapinto));
        when(resultadoSesionRepo.countByPilotoIdAndEliminadoFalse(1L)).thenReturn(12L);

        assertThatThrownBy(() -> pilotos.eliminar(1L, ADMIN_ID))
                .isInstanceOfSatisfying(RuleBusinessException.class, conEstado(HttpStatus.CONFLICT))
                .hasMessageContaining("12 resultados y 0 tripulaciones");
        assertThat(colapinto.isEliminado()).isFalse();
    }

    @Test
    void eliminarPiloto_queEsCopilotoDeUnaTripulacion_lanza409() {
        Piloto copiloto = piloto(2L, "Martijn", "Wydaeghe");
        when(pilotoRepo.findById(2L)).thenReturn(Optional.of(copiloto));
        when(tripulacionRepo.countByCopilotoIdAndEliminadoFalse(2L)).thenReturn(1L);

        assertThatThrownBy(() -> pilotos.eliminar(2L, ADMIN_ID))
                .isInstanceOfSatisfying(RuleBusinessException.class, conEstado(HttpStatus.CONFLICT))
                .hasMessageContaining("0 resultados y 1 tripulaciones");
        assertThat(copiloto.isEliminado()).isFalse();
    }

    @Test
    void eliminarPiloto_sinResultadosNiTripulaciones_esBajaLogica() {
        Piloto reserva = piloto(3L, null, "Reserva");
        when(pilotoRepo.findById(3L)).thenReturn(Optional.of(reserva));

        pilotos.eliminar(3L, ADMIN_ID);

        assertThat(reserva.isEliminado()).isTrue();
        assertThat(reserva.getActualizadoPor()).isSameAs(admin);
        verify(pilotoRepo, never()).delete(any());
    }

    @Test
    void modificarPiloto_dadoDeBaja_lanza404() {
        when(pilotoRepo.findById(3L)).thenReturn(Optional.of(dadoDeBaja(piloto(3L, null, "Reserva"))));

        assertThatThrownBy(() -> pilotos.modificar(3L, requestPiloto(null, null), ADMIN_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------------------------------------------------------- equipos

    @Test
    void crearEquipo_normalizaLosTextosYRegistraLaAuditoria() {
        when(equipoRepo.save(any(Equipo.class))).thenAnswer(inv -> inv.getArgument(0));

        EquipoDtos.Registro creado = equipos.crear(requestEquipo(), ADMIN_ID);

        assertThat(creado.nombre()).isEqualTo("Canning Motorsport");
        assertThat(creado.nombreCorto()).isEqualTo("Ford");
        assertThat(creado.nacionalidad()).isNull();
        assertThat(creado.sede()).isEqualTo("Salto");
        assertThat(creado.auditoria().creadoPor()).isEqualTo(ADMIN_EMAIL);
    }

    @Test
    void eliminarEquipo_conTripulacionesActivas_lanza409() {
        Equipo toyota = equipo(5L, "Toyota Gazoo Racing WRT");
        when(equipoRepo.findById(5L)).thenReturn(Optional.of(toyota));
        when(tripulacionRepo.countByEquipoIdAndEliminadoFalse(5L)).thenReturn(3L);

        assertThatThrownBy(() -> equipos.eliminar(5L, ADMIN_ID))
                .isInstanceOfSatisfying(RuleBusinessException.class, conEstado(HttpStatus.CONFLICT))
                .hasMessageContaining("0 resultados y 3 tripulaciones");
        assertThat(toyota.isEliminado()).isFalse();
    }

    @Test
    void eliminarEquipo_sinDependencias_esBajaLogica() {
        Equipo equipo = equipo(6L, "Equipo sin carreras");
        when(equipoRepo.findById(6L)).thenReturn(Optional.of(equipo));

        equipos.eliminar(6L, ADMIN_ID);

        assertThat(equipo.isEliminado()).isTrue();
        assertThat(equipo.getActualizadoPor()).isSameAs(admin);
        verify(resultadoSesionRepo).countByEquipoIdAndEliminadoFalse(6L);
    }

    @Test
    void listarEquipos_buscaPorPatronEnMinusculas() {
        when(equipoRepo.buscarPorTexto(eq("%ford%"), any(Pageable.class))).thenReturn(List.of(equipo(5L, "Ford")));

        List<EquipoDtos.Registro> encontrados = equipos.listar("FORD", 50);

        assertThat(encontrados).extracting(EquipoDtos.Registro::nombre).containsExactly("Ford");
    }
}

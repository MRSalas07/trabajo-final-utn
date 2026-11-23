package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.dtos.gestionDatos.ParticipacionDtos;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Catalogo;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.model.Equipo;
import com.tfi.motorsport.backend.model.EquipoPiloto;
import com.tfi.motorsport.backend.model.Piloto;
import com.tfi.motorsport.backend.model.Usuario;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.repositories.EquipoJPARepository;
import com.tfi.motorsport.backend.repositories.EquipoPilotoJPARepository;
import com.tfi.motorsport.backend.repositories.PilotoJPARepository;
import com.tfi.motorsport.backend.repositories.UsuarioJPARepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static com.tfi.motorsport.backend.services.gestion.Impl.GestionDePrueba.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ABM de participaciones (equipo_x_piloto): una por piloto, equipo, categoría, temporada y rol
 * (si estaba dada de baja se reactiva), rol PILOTO por defecto, marca/motor de TC y el registro
 * automático que usan los resultados y las tripulaciones.
 */
@ExtendWith(MockitoExtension.class)
class ParticipacionGestionServiceImplTest {

    @Mock
    private EquipoPilotoJPARepository participacionRepo;

    @Mock
    private CategoriaJPARepository categoriaRepo;

    @Mock
    private PilotoJPARepository pilotoRepo;

    @Mock
    private EquipoJPARepository equipoRepo;

    @Mock
    private UsuarioJPARepository usuarioRepo;

    private final Usuario admin = admin();
    private final Categoria tc = categoria(3L, "TC");
    private final Piloto canapino = piloto(1L, "Agustín", "Canapino");
    private final Equipo canning = equipo(5L, "Canning Motorsport");
    private ParticipacionGestionServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(usuarioRepo.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        service = new ParticipacionGestionServiceImpl(participacionRepo, categoriaRepo, pilotoRepo, equipoRepo,
                new SoporteGestion(usuarioRepo));
    }

    private static ParticipacionDtos.Request request(String rol, String marca) {
        return new ParticipacionDtos.Request(5L, 1L, 3L, 2026, 12, rol, null, null, marca);
    }

    /** Categoría, piloto y equipo existen y están activos. */
    private void referenciasActivas() {
        when(categoriaRepo.findById(3L)).thenReturn(Optional.of(tc));
        when(pilotoRepo.findById(1L)).thenReturn(Optional.of(canapino));
        when(equipoRepo.findById(5L)).thenReturn(Optional.of(canning));
    }

    /** Referencias que se asignan al guardar la participación. */
    private void referenciasParaGuardar() {
        when(categoriaRepo.getReferenceById(3L)).thenReturn(tc);
        when(pilotoRepo.getReferenceById(1L)).thenReturn(canapino);
        when(equipoRepo.getReferenceById(5L)).thenReturn(canning);
    }

    private void participacionExistente(EquipoPiloto ep) {
        when(participacionRepo.findByEquipoIdAndPilotoIdAndCategoriaIdAndTemporadaAndRol(
                5L, 1L, 3L, 2026, Catalogo.ROL_PILOTO)).thenReturn(Optional.ofNullable(ep));
    }

    @Test
    void crear_sinRol_esPilotoYGuardaLaMarcaRecortada() {
        referenciasActivas();
        referenciasParaGuardar();
        participacionExistente(null);
        when(participacionRepo.save(any(EquipoPiloto.class))).thenAnswer(inv -> inv.getArgument(0));

        ParticipacionDtos.Registro creada = service.crear(request(null, "  Ford "), ADMIN_ID);

        assertThat(creada.rol()).isEqualTo(Catalogo.ROL_PILOTO);
        assertThat(creada.marca()).isEqualTo("Ford");
        assertThat(creada.piloto()).isEqualTo("Agustín Canapino");
        assertThat(creada.equipo()).isEqualTo("Canning Motorsport");
        assertThat(creada.categoria()).isEqualTo("TC");
        assertThat(creada.temporada()).isEqualTo(2026);
        assertThat(creada.auditoria().creadoPor()).isEqualTo(ADMIN_EMAIL);
    }

    @Test
    void crear_marcaEnBlanco_quedaVaciaParaUsarLaDelEquipo() {
        referenciasActivas();
        referenciasParaGuardar();
        participacionExistente(null);
        when(participacionRepo.save(any(EquipoPiloto.class))).thenAnswer(inv -> inv.getArgument(0));

        ParticipacionDtos.Registro creada = service.crear(request(Catalogo.ROL_PILOTO, "   "), ADMIN_ID);

        assertThat(creada.marca()).isNull();
    }

    @Test
    void crear_participacionActivaRepetida_lanza409SinGuardar() {
        referenciasActivas();
        participacionExistente(participacion(9L, canning, canapino, tc, 2026, Catalogo.ROL_PILOTO));

        assertThatThrownBy(() -> service.crear(request(null, null), ADMIN_ID))
                .isInstanceOfSatisfying(RuleBusinessException.class, conEstado(HttpStatus.CONFLICT))
                .hasMessage("El piloto ya figura en ese equipo para la temporada 2026");
        verify(participacionRepo, never()).save(any());
    }

    @Test
    void crear_participacionDadaDeBaja_seReactivaConLosDatosNuevos() {
        EquipoPiloto vieja = dadoDeBaja(participacion(9L, canning, canapino, tc, 2026, Catalogo.ROL_PILOTO));
        referenciasActivas();
        referenciasParaGuardar();
        participacionExistente(vieja);
        when(participacionRepo.save(vieja)).thenReturn(vieja);

        ParticipacionDtos.Registro reactivada = service.crear(request(Catalogo.ROL_PILOTO, "Chevrolet"), ADMIN_ID);

        assertThat(reactivada.id()).isEqualTo(9L);
        assertThat(vieja.isEliminado()).isFalse();
        assertThat(vieja.getMarca()).isEqualTo("Chevrolet");
        assertThat(vieja.getNumero()).isEqualTo(12);
    }

    @Test
    void crear_conUnPilotoDadoDeBaja_lanza404() {
        when(categoriaRepo.findById(3L)).thenReturn(Optional.of(tc));
        when(pilotoRepo.findById(1L)).thenReturn(Optional.of(dadoDeBaja(piloto(1L, "Agustín", "Canapino"))));

        assertThatThrownBy(() -> service.crear(request(null, null), ADMIN_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No se encontró el piloto con ID 1");
        verifyNoInteractions(participacionRepo);
    }

    @Test
    void modificar_sinRol_conservaElRolActualYRegistraQuienLaCambio() {
        EquipoPiloto ep = participacion(9L, canning, canapino, tc, 2026, Catalogo.ROL_COPILOTO);
        when(participacionRepo.findById(9L)).thenReturn(Optional.of(ep));
        referenciasActivas();
        referenciasParaGuardar();

        ParticipacionDtos.Registro modificada = service.modificar(9L, request(null, "Toyota"), ADMIN_ID);

        assertThat(modificada.rol()).isEqualTo(Catalogo.ROL_COPILOTO);
        assertThat(modificada.marca()).isEqualTo("Toyota");
        assertThat(ep.getActualizadoPor()).isSameAs(admin);
    }

    @Test
    void eliminar_esBajaLogica() {
        EquipoPiloto ep = participacion(9L, canning, canapino, tc, 2026, Catalogo.ROL_PILOTO);
        when(participacionRepo.findById(9L)).thenReturn(Optional.of(ep));

        service.eliminar(9L, ADMIN_ID);

        assertThat(ep.isEliminado()).isTrue();
        assertThat(ep.getActualizadoPor()).isSameAs(admin);
    }

    // ---------------------------------------------------------------- registro automático

    @Test
    void registrarSiNoExiste_sinParticipacion_laCreaConAuditoria() {
        participacionExistente(null);
        referenciasParaGuardar();
        ArgumentCaptor<EquipoPiloto> guardada = ArgumentCaptor.forClass(EquipoPiloto.class);

        service.registrarSiNoExiste(5L, 1L, 3L, 2026, 12, Catalogo.ROL_PILOTO, ADMIN_ID);

        verify(participacionRepo).save(guardada.capture());
        assertThat(guardada.getValue().getPiloto()).isSameAs(canapino);
        assertThat(guardada.getValue().getEquipo()).isSameAs(canning);
        assertThat(guardada.getValue().getTemporada()).isEqualTo(2026);
        assertThat(guardada.getValue().getNumero()).isEqualTo(12);
        assertThat(guardada.getValue().getCreadoPor()).isSameAs(admin);
    }

    @Test
    void registrarSiNoExiste_dadaDeBaja_laReactivaSinCrearOtra() {
        EquipoPiloto vieja = dadoDeBaja(participacion(9L, canning, canapino, tc, 2026, Catalogo.ROL_PILOTO));
        participacionExistente(vieja);

        service.registrarSiNoExiste(5L, 1L, 3L, 2026, 12, Catalogo.ROL_PILOTO, ADMIN_ID);

        assertThat(vieja.isEliminado()).isFalse();
        assertThat(vieja.getActualizadoPor()).isSameAs(admin);
        verify(participacionRepo, never()).save(any());
    }

    @Test
    void registrarSiNoExiste_yaActiva_noLaModifica() {
        EquipoPiloto activa = participacion(9L, canning, canapino, tc, 2026, Catalogo.ROL_PILOTO);
        participacionExistente(activa);

        service.registrarSiNoExiste(5L, 1L, 3L, 2026, 12, Catalogo.ROL_PILOTO, ADMIN_ID);

        assertThat(activa.getActualizadoPor()).isNull();
        verify(participacionRepo, never()).save(any());
    }
}

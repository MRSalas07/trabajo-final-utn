package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.dtos.gestionDatos.TemporadaDtos;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.model.Temporada;
import com.tfi.motorsport.backend.model.Usuario;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.repositories.TemporadaJPARepository;
import com.tfi.motorsport.backend.repositories.UsuarioJPARepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.tfi.motorsport.backend.services.gestion.Impl.GestionDePrueba.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ABM de temporadas: una sola temporada por categoría y año (si estaba dada de baja se reactiva),
 * fechas coherentes, nombre por defecto y auditoría.
 */
@ExtendWith(MockitoExtension.class)
class TemporadaGestionServiceImplTest {

    @Mock
    private TemporadaJPARepository temporadaRepo;

    @Mock
    private CategoriaJPARepository categoriaRepo;

    @Mock
    private UsuarioJPARepository usuarioRepo;

    private final Usuario admin = admin();
    private final Categoria f1 = categoria(1L, "F1");
    private TemporadaGestionServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(usuarioRepo.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        service = new TemporadaGestionServiceImpl(temporadaRepo, categoriaRepo, new SoporteGestion(usuarioRepo));
    }

    private static TemporadaDtos.Request request(int anio, String nombre) {
        return new TemporadaDtos.Request(1L, anio, nombre, LocalDate.of(anio, 3, 1), LocalDate.of(anio, 12, 7));
    }

    @Test
    void crear_temporadaNueva_seGuardaConNombrePorDefectoYAuditoria() {
        when(categoriaRepo.findById(1L)).thenReturn(Optional.of(f1));
        when(temporadaRepo.findByCategoriaIdAndAnio(1L, 2026)).thenReturn(Optional.empty());
        when(temporadaRepo.save(any(Temporada.class))).thenAnswer(inv -> inv.getArgument(0));

        TemporadaDtos.Registro creada = service.crear(request(2026, "   "), ADMIN_ID);

        assertThat(creada.categoria()).isEqualTo("F1");
        assertThat(creada.anio()).isEqualTo(2026);
        assertThat(creada.nombre()).isEqualTo("F1 2026");
        assertThat(creada.auditoria().creadoPor()).isEqualTo(ADMIN_EMAIL);
        assertThat(creada.auditoria().actualizadoPor()).isEqualTo(ADMIN_EMAIL);
    }

    @Test
    void crear_temporadaQueYaExiste_lanza409SinGuardar() {
        when(categoriaRepo.findById(1L)).thenReturn(Optional.of(f1));
        when(temporadaRepo.findByCategoriaIdAndAnio(1L, 2025)).thenReturn(Optional.of(temporada(5L, f1, 2025)));

        assertThatThrownBy(() -> service.crear(request(2025, null), ADMIN_ID))
                .isInstanceOfSatisfying(RuleBusinessException.class, conEstado(HttpStatus.CONFLICT))
                .hasMessage("Ya existe la temporada 2025 de F1");
        verify(temporadaRepo, never()).save(any());
    }

    @Test
    void crear_temporadaDadaDeBaja_seReactivaEnLugarDeDuplicarla() {
        Temporada vieja = dadoDeBaja(temporada(5L, f1, 2024));
        when(categoriaRepo.findById(1L)).thenReturn(Optional.of(f1));
        when(temporadaRepo.findByCategoriaIdAndAnio(1L, 2024)).thenReturn(Optional.of(vieja));
        when(temporadaRepo.save(vieja)).thenReturn(vieja);

        TemporadaDtos.Registro reactivada = service.crear(request(2024, "Temporada 2024"), ADMIN_ID);

        assertThat(reactivada.id()).isEqualTo(5L);
        assertThat(vieja.isEliminado()).isFalse();
        assertThat(vieja.getNombre()).isEqualTo("Temporada 2024");
        assertThat(vieja.getCreadoPor()).isSameAs(admin);
    }

    @Test
    void crear_fechaFinAnteriorAlInicio_lanza400SinGuardar() {
        when(categoriaRepo.findById(1L)).thenReturn(Optional.of(f1));
        TemporadaDtos.Request r = new TemporadaDtos.Request(1L, 2026, null,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 3, 1));

        assertThatThrownBy(() -> service.crear(r, ADMIN_ID))
                .isInstanceOfSatisfying(RuleBusinessException.class, conEstado(HttpStatus.BAD_REQUEST));
        verify(temporadaRepo, never()).save(any());
    }

    @Test
    void crear_enUnaCategoriaDadaDeBaja_lanza404() {
        when(categoriaRepo.findById(1L)).thenReturn(Optional.of(dadoDeBaja(categoria(1L, "F1"))));

        assertThatThrownBy(() -> service.crear(request(2026, null), ADMIN_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(temporadaRepo);
    }

    @Test
    void modificar_aUnAnioQueYaTieneOtraTemporada_lanza409YNoCambiaNada() {
        Temporada t2025 = temporada(5L, f1, 2025);
        when(temporadaRepo.findById(5L)).thenReturn(Optional.of(t2025));
        when(categoriaRepo.findById(1L)).thenReturn(Optional.of(f1));
        when(temporadaRepo.findByCategoriaIdAndAnio(1L, 2026)).thenReturn(Optional.of(temporada(6L, f1, 2026)));

        assertThatThrownBy(() -> service.modificar(5L, request(2026, null), ADMIN_ID))
                .isInstanceOfSatisfying(RuleBusinessException.class, conEstado(HttpStatus.CONFLICT))
                .hasMessage("Ya existe la temporada 2026 de F1");
        assertThat(t2025.getAnio()).isEqualTo(2025);
        assertThat(t2025.getActualizadoPor()).isNull();
    }

    @Test
    void modificar_mismoAnioYCategoria_actualizaDatosYAuditoriaSinBuscarDuplicados() {
        Temporada t2025 = temporada(5L, f1, 2025);
        when(temporadaRepo.findById(5L)).thenReturn(Optional.of(t2025));

        TemporadaDtos.Registro modificada = service.modificar(5L, request(2025, " Mundial 2025 "), ADMIN_ID);

        assertThat(modificada.nombre()).isEqualTo("Mundial 2025");
        assertThat(t2025.getFechaFin()).isEqualTo(LocalDate.of(2025, 12, 7));
        assertThat(t2025.getActualizadoPor()).isSameAs(admin);
        verify(temporadaRepo, never()).findByCategoriaIdAndAnio(any(), any());
    }

    @Test
    void eliminar_esBajaLogicaConElUsuarioQueLaHizo() {
        Temporada t2025 = temporada(5L, f1, 2025);
        when(temporadaRepo.findById(5L)).thenReturn(Optional.of(t2025));

        service.eliminar(5L, ADMIN_ID);

        assertThat(t2025.isEliminado()).isTrue();
        assertThat(t2025.getActualizadoPor()).isSameAs(admin);
        verify(temporadaRepo, never()).delete(any());
    }

    @Test
    void eliminar_temporadaYaDadaDeBaja_lanza404() {
        when(temporadaRepo.findById(5L)).thenReturn(Optional.of(dadoDeBaja(temporada(5L, f1, 2025))));

        assertThatThrownBy(() -> service.eliminar(5L, ADMIN_ID)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listar_devuelveLasTemporadasDeLaCategoriaConSuAuditoria() {
        Temporada t2025 = temporada(5L, f1, 2025);
        t2025.setCreadoPor(admin);
        when(categoriaRepo.findById(1L)).thenReturn(Optional.of(f1));
        when(temporadaRepo.findByCategoriaIdAndEliminadoFalseOrderByAnioDesc(1L)).thenReturn(List.of(t2025));

        List<TemporadaDtos.Registro> temporadas = service.listar(1L);

        assertThat(temporadas).hasSize(1);
        assertThat(temporadas.get(0).anio()).isEqualTo(2025);
        assertThat(temporadas.get(0).auditoria().creadoPor()).isEqualTo(ADMIN_EMAIL);
        assertThat(temporadas.get(0).auditoria().actualizadoPor()).isNull();
    }
}

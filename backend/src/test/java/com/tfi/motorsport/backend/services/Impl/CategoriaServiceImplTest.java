package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.dtos.categoria.CategoriaCreateDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaUpdateDto;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

/**
 * Pruebas unitarias del servicio de categorías.
 * Los repositorios y servicios de los que depende se reemplazan por mocks: no se usa base de datos.
 * Cada prueba sigue el patrón AAA: Arrange (preparar), Act (ejecutar), Assert (verificar).
 */
@ExtendWith(MockitoExtension.class)
class CategoriaServiceImplTest {

    @Mock
    private CategoriaJPARepository categoriaRepo;

    @InjectMocks
    private CategoriaServiceImpl service;

    /** Categoría de ejemplo para las pruebas. */
    private static Categoria categoria(Long id, String siglas, boolean activo, boolean eliminado) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setNombre("Categoría " + siglas);
        c.setSiglas(siglas);
        c.setActivo(activo);
        c.setEliminado(eliminado);
        return c;
    }
    @Test
    void obtenerCategorias_devuelveLasCategoriasConvertidasADto() {
        // Arrange
        when(categoriaRepo.findAllByEliminadoFalseOrderByIdAsc())
                .thenReturn(List.of(categoria(1L, "F1", true, false), categoria(2L, "WRC", true, false)));

        // Act
        List<CategoriaDto> resultado = service.obtenerCategorias();

        // Assert
        assertThat(resultado).extracting(CategoriaDto::siglas).containsExactly("F1", "WRC");
    }

    @Test
    void obtenerCategoriasPorFiltro_pasaElFiltroAlRepositorio() {
        // Arrange
        when(categoriaRepo.findAllByActivoAndEliminadoFalseOrderByIdAsc(false))
                .thenReturn(List.of(categoria(3L, "TC", false, false)));

        // Act
        List<CategoriaDto> resultado = service.obtenerCategoriasPorFiltro(false);

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).activo()).isFalse();
    }

    @Test
    void obtenerCategoria_existente_devuelveSusDatos() {
        // Arrange
        when(categoriaRepo.findById(1L)).thenReturn(Optional.of(categoria(1L, "F1", true, false)));

        // Act
        CategoriaDto resultado = service.obtenerCategoria(1L);

        // Assert
        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.siglas()).isEqualTo("F1");
    }

    @Test
    void obtenerCategoria_inexistente_lanzaRecursoNoEncontrado() {
        // Arrange
        when(categoriaRepo.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.obtenerCategoria(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerCategoria_dadaDeBaja_lanzaRecursoNoEncontrado() {
        // Arrange
        when(categoriaRepo.findById(5L)).thenReturn(Optional.of(categoria(5L, "OLD", true, true)));

        // Act + Assert
        assertThatThrownBy(() -> service.obtenerCategoria(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ------------------------------------------------------------------ alta

    @Test
    void crearCategoria_nueva_normalizaLasSiglasYLaGuardaActiva() {
        // Arrange
        CategoriaCreateDto dto = new CategoriaCreateDto("  Fórmula 1 ", " f1 ", "Máxima categoría", "Internacional", null);
        when(categoriaRepo.findBySiglasIgnoreCase("F1")).thenReturn(Optional.empty());
        when(categoriaRepo.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CategoriaDto resultado = service.crearCategoria(dto);

        // Assert
        assertThat(resultado.siglas()).isEqualTo("F1");
        assertThat(resultado.nombre()).isEqualTo("Fórmula 1");
        assertThat(resultado.activo()).isTrue();
        verify(categoriaRepo).save(any(Categoria.class));
    }

    @Test
    void crearCategoria_conSiglasExistentes_lanzaConflictoYNoGuarda() {
        // Arrange
        CategoriaCreateDto dto = new CategoriaCreateDto("Fórmula 1", "F1", null, null, null);
        when(categoriaRepo.findBySiglasIgnoreCase("F1")).thenReturn(Optional.of(categoria(1L, "F1", true, false)));

        // Act + Assert
        assertThatThrownBy(() -> service.crearCategoria(dto))
                .isInstanceOf(RuleBusinessException.class)
                .satisfies(ex -> assertThat(((RuleBusinessException) ex).getEstado()).isEqualTo(HttpStatus.CONFLICT));
        verify(categoriaRepo, never()).save(any());
    }

    @Test
    void crearCategoria_conSiglasDeUnaDadaDeBaja_reactivaLaExistente() {
        // Arrange
        Categoria dadaDeBaja = categoria(7L, "TC", false, true);
        CategoriaCreateDto dto = new CategoriaCreateDto("Turismo Carretera", "TC", null, "Argentina", null);
        when(categoriaRepo.findBySiglasIgnoreCase("TC")).thenReturn(Optional.of(dadaDeBaja));
        when(categoriaRepo.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CategoriaDto resultado = service.crearCategoria(dto);

        // Assert: se reutiliza el mismo registro (mismo id) en lugar de crear uno nuevo
        ArgumentCaptor<Categoria> guardada = ArgumentCaptor.forClass(Categoria.class);
        verify(categoriaRepo).save(guardada.capture());
        assertThat(guardada.getValue().isEliminado()).isFalse();
        assertThat(resultado.id()).isEqualTo(7L);
        assertThat(resultado.activo()).isTrue();
    }

    // ------------------------------------------------------------------ modificación

    @Test
    void modificarCategoria_actualizaLosDatosYElEstado() {
        // Arrange
        Categoria existente = categoria(1L, "F1", true, false);
        CategoriaUpdateDto dto = new CategoriaUpdateDto("Formula One", "f1", "Nueva descripción", "Internacional", null, false);
        when(categoriaRepo.findById(1L)).thenReturn(Optional.of(existente));
        when(categoriaRepo.findBySiglasIgnoreCase("F1")).thenReturn(Optional.of(existente));
        when(categoriaRepo.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CategoriaDto resultado = service.modificarCategoria(1L, dto);

        // Assert
        assertThat(resultado.nombre()).isEqualTo("Formula One");
        assertThat(resultado.descripcion()).isEqualTo("Nueva descripción");
        assertThat(resultado.activo()).isFalse();
    }

    @Test
    void modificarCategoria_conSiglasDeOtraCategoria_lanzaConflicto() {
        // Arrange
        CategoriaUpdateDto dto = new CategoriaUpdateDto("Fórmula 1", "WRC", null, null, null, null);
        when(categoriaRepo.findById(1L)).thenReturn(Optional.of(categoria(1L, "F1", true, false)));
        when(categoriaRepo.findBySiglasIgnoreCase("WRC")).thenReturn(Optional.of(categoria(2L, "WRC", true, false)));

        // Act + Assert
        assertThatThrownBy(() -> service.modificarCategoria(1L, dto))
                .isInstanceOf(RuleBusinessException.class);
        verify(categoriaRepo, never()).save(any());
    }
}
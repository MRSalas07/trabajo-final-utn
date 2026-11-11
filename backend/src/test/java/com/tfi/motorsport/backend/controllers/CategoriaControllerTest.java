package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.dtos.categoria.CategoriaCreateDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaUpdateDto;
import com.tfi.motorsport.backend.services.ICategoriaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del controlador de categorías.
 * Se llama directamente a los métodos del controlador con el servicio simulado (mock):
 * se verifica que delegue en el servicio correcto y que responda con el código HTTP esperado.
 * Las reglas de seguridad y las validaciones de entrada se prueban en SeguridadYErroresTest.
 */
@ExtendWith(MockitoExtension.class)
class CategoriaControllerTest {
    @Mock
    private ICategoriaService categoriaService;

    @InjectMocks
    private CategoriaController controller;

    private static final CategoriaDto F1 =
            new CategoriaDto(1L, "Fórmula 1", "F1", "Máxima categoría", "Internacional", null, true);

    @Test
    void obtenerCategorias_sinFiltro_devuelveTodasCon200() {
        // Arrange
        when(categoriaService.obtenerCategorias()).thenReturn(List.of(F1));

        // Act
        ResponseEntity<List<CategoriaDto>> respuesta = controller.obtenerCategorias(null);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(F1);
        verify(categoriaService, never()).obtenerCategoriasPorFiltro(true);
    }

    @Test
    void obtenerCategorias_conFiltro_usaLaBusquedaPorEstado() {
        // Arrange
        when(categoriaService.obtenerCategoriasPorFiltro(true)).thenReturn(List.of(F1));

        // Act
        ResponseEntity<List<CategoriaDto>> respuesta = controller.obtenerCategorias(true);

        // Assert
        assertThat(respuesta.getBody()).containsExactly(F1);
        verify(categoriaService, never()).obtenerCategorias();
    }

    @Test
    void obtenerPorId_devuelveLaCategoriaCon200() {
        // Arrange
        when(categoriaService.obtenerCategoria(1L)).thenReturn(F1);

        // Act
        ResponseEntity<CategoriaDto> respuesta = controller.obtenerPorId(1L);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(F1);
    }

    @Test
    void crearCategoria_devuelve201ConLaCategoriaCreada() {
        // Arrange
        CategoriaCreateDto dto = new CategoriaCreateDto("Fórmula 1", "F1", "Máxima categoría", "Internacional", null);
        when(categoriaService.crearCategoria(dto)).thenReturn(F1);

        // Act
        ResponseEntity<CategoriaDto> respuesta = controller.crearCategoria(dto);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(F1);
    }

    @Test
    void actualizarCategoria_devuelve200ConLaCategoriaModificada() {
        // Arrange
        CategoriaUpdateDto dto = new CategoriaUpdateDto("Formula One", "F1", null, null, null, false);
        CategoriaDto modificada = new CategoriaDto(1L, "Formula One", "F1", null, null, null, false);
        when(categoriaService.modificarCategoria(1L, dto)).thenReturn(modificada);

        // Act
        ResponseEntity<CategoriaDto> respuesta = controller.actualizarCategoria(1L, dto);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(modificada);
    }
}
package com.tfi.motorsport.backend.controllers.gestion;

import com.tfi.motorsport.backend.dtos.gestionDatos.CategoriaDtos;
import com.tfi.motorsport.backend.dtos.gestionDatos.PalmaresDtos;
import com.tfi.motorsport.backend.services.gestion.ICategoriaGestionService;
import com.tfi.motorsport.backend.services.gestion.IPalmaresGestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de los controladores de gestión de solo lectura: categorías y palmarés.
 * Se llama directamente a los métodos del controlador con el servicio simulado (mock).
 * Cada prueba sigue el patrón AAA: Arrange (preparar), Act (ejecutar), Assert (verificar).
 */
@ExtendWith(MockitoExtension.class)
class CategoriaYPalmaresGestionControllerTest {

    @Mock
    private ICategoriaGestionService categoriaServicio;

    @Mock
    private IPalmaresGestionService palmaresServicio;

    @Test
    void categorias_listar_devuelveLasCategoriasConSuAuditoria() {
        // Arrange
        CategoriaDtos.Registro registro = mock(CategoriaDtos.Registro.class);
        when(categoriaServicio.listar()).thenReturn(List.of(registro));
        CategoriaGestionController controller = new CategoriaGestionController(categoriaServicio);

        // Act
        List<CategoriaDtos.Registro> resultado = controller.listar();

        // Assert
        assertThat(resultado).containsExactly(registro);
        verify(categoriaServicio).listar();
    }

    @Test
    void palmares_listar_pideElPalmaresDeLaCategoriaIndicada() {
        // Arrange
        PalmaresDtos.Registro registro = mock(PalmaresDtos.Registro.class);
        when(palmaresServicio.listar(5L)).thenReturn(List.of(registro));
        PalmaresGestionController controller = new PalmaresGestionController(palmaresServicio);

        // Act
        List<PalmaresDtos.Registro> resultado = controller.listar(5L);

        // Assert
        assertThat(resultado).containsExactly(registro);
        verify(palmaresServicio).listar(5L);
    }
}

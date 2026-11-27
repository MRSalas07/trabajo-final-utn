package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.dtos.comun.PaginaDto;
import com.tfi.motorsport.backend.dtos.equipo.EquipoResumenDto;
import com.tfi.motorsport.backend.dtos.equipo.PerfilEquipoDto;
import com.tfi.motorsport.backend.dtos.piloto.PerfilPilotoDto;
import com.tfi.motorsport.backend.dtos.piloto.PilotoResumenDto;
import com.tfi.motorsport.backend.services.IEquipoService;
import com.tfi.motorsport.backend.services.IPilotoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de los controladores de equipos y pilotos (listado y ficha).
 * Se llama directamente a los métodos del controlador con el servicio simulado (mock).
 * Cada prueba sigue el patrón AAA: Arrange (preparar), Act (ejecutar), Assert (verificar).
 */
@ExtendWith(MockitoExtension.class)
class EquipoYPilotoControllerTest {

    @Mock
    private IEquipoService equipoService;

    @Mock
    private IPilotoService pilotoService;

    @Test
    void equipos_devuelveLosEquiposDeLaCategoriaYElAnio() {
        // Arrange
        EquipoResumenDto equipo = mock(EquipoResumenDto.class);
        when(equipoService.equiposDeCategoria("F1", 2025)).thenReturn(List.of(equipo));
        EquipoController controller = new EquipoController(equipoService);

        // Act
        List<EquipoResumenDto> resultado = controller.equipos("F1", 2025);

        // Assert
        assertThat(resultado).containsExactly(equipo);
    }

    @Test
    void equipo_devuelveLaFichaDelEquipo() {
        // Arrange
        PerfilEquipoDto perfil = mock(PerfilEquipoDto.class);
        when(equipoService.perfil(10L)).thenReturn(perfil);
        EquipoController controller = new EquipoController(equipoService);

        // Act
        PerfilEquipoDto resultado = controller.equipo(10L);

        // Assert
        assertThat(resultado).isSameAs(perfil);
    }

    @Test
    @SuppressWarnings("unchecked")
    void pilotos_pasaLosFiltrosYLaPaginaAlServicio() {
        // Arrange
        PaginaDto<PilotoResumenDto> pagina = mock(PaginaDto.class);
        when(pilotoService.buscarPilotos("alo", "F1", 2025, 1, 24)).thenReturn(pagina);
        PilotoController controller = new PilotoController(pilotoService);

        // Act
        PaginaDto<PilotoResumenDto> resultado = controller.pilotos("alo", "F1", 2025, 1, 24);

        // Assert
        assertThat(resultado).isSameAs(pagina);
    }

    @Test
    void piloto_devuelveLaFichaDelPiloto() {
        // Arrange
        PerfilPilotoDto perfil = mock(PerfilPilotoDto.class);
        when(pilotoService.perfil(1L)).thenReturn(perfil);
        PilotoController controller = new PilotoController(pilotoService);

        // Act
        PerfilPilotoDto resultado = controller.piloto(1L);

        // Assert
        assertThat(resultado).isSameAs(perfil);
    }
}

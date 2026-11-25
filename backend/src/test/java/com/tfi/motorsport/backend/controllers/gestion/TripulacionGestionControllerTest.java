package com.tfi.motorsport.backend.controllers.gestion;

import com.tfi.motorsport.backend.dtos.gestionDatos.TripulacionDtos;
import com.tfi.motorsport.backend.services.gestion.ITripulacionGestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del controlador de gestión de tripulaciones.
 * Se llama directamente a los métodos del controlador con el servicio simulado (mock):
 * se verifica que delegue en el servicio y que pase el id del administrador para la auditoría.
 * Las reglas de acceso y los códigos HTTP se prueban en GestionRutasTest.
 * Cada prueba sigue el patrón AAA: Arrange (preparar), Act (ejecutar), Assert (verificar).
 */
@ExtendWith(MockitoExtension.class)
class TripulacionGestionControllerTest {

    /** Token del administrador que hace la operación (id 7): queda en la auditoría. */
    private static final Jwt ADMIN = Jwt.withTokenValue("token").header("alg", "none").subject("7").build();
    private static final Long ID_ADMIN = 7L;

    @Mock
    private ITripulacionGestionService servicio;

    @InjectMocks
    private TripulacionGestionController controller;

    @Test
    void listar_delegaEnElServicioYDevuelveLosRegistros() {
        // Arrange
        TripulacionDtos.Registro registro = mock(TripulacionDtos.Registro.class);
        when(servicio.listar(5L, 2025)).thenReturn(List.of(registro));

        // Act
        List<TripulacionDtos.Registro> resultado = controller.listar(5L, 2025);

        // Assert
        assertThat(resultado).containsExactly(registro);
    }

    @Test
    void crear_pasaElIdDelAdministradorYDevuelveElRegistroCreado() {
        // Arrange
        TripulacionDtos.Request pedido = mock(TripulacionDtos.Request.class);
        TripulacionDtos.Registro creado = mock(TripulacionDtos.Registro.class);
        when(servicio.crear(pedido, ID_ADMIN)).thenReturn(creado);

        // Act
        TripulacionDtos.Registro resultado = controller.crear(pedido, ADMIN);

        // Assert
        assertThat(resultado).isSameAs(creado);
        verify(servicio).crear(pedido, ID_ADMIN);
    }

    @Test
    void modificar_pasaElIdDelRegistroYElDelAdministrador() {
        // Arrange
        TripulacionDtos.Request pedido = mock(TripulacionDtos.Request.class);
        TripulacionDtos.Registro modificado = mock(TripulacionDtos.Registro.class);
        when(servicio.modificar(3L, pedido, ID_ADMIN)).thenReturn(modificado);

        // Act
        TripulacionDtos.Registro resultado = controller.modificar(3L, pedido, ADMIN);

        // Assert
        assertThat(resultado).isSameAs(modificado);
        verify(servicio).modificar(3L, pedido, ID_ADMIN);
    }

    @Test
    void eliminar_daDeBajaElRegistroConElIdDelAdministrador() {
        // Arrange (el servicio simulado no devuelve nada: la baja es lógica)

        // Act
        controller.eliminar(3L, ADMIN);

        // Assert
        verify(servicio).eliminar(3L, ID_ADMIN);
    }
}

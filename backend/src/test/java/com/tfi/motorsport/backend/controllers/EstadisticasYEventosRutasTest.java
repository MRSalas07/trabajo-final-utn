package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.services.IEstadisticaService;
import com.tfi.motorsport.backend.services.IEventoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZoneId;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rutas de los controladores de estadísticas y de eventos con los servicios simulados (@MockitoBean)
 * y la seguridad real: validación de los límites de los parámetros, traducción de errores a HTTP
 * y acceso restringido al recálculo del palmarés.
 * Cada prueba sigue el patrón AAA: Arrange (preparar), Act (ejecutar), Assert (verificar).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EstadisticasYEventosRutasTest {

    private static final SimpleGrantedAuthority ROL_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private IEstadisticaService estadisticaService;

    @MockitoBean
    private IEventoService eventoService;

    @Test
    void campeonatoDePilotos_esPublicoYDevuelveUnaLista() throws Exception {
        // Arrange (el servicio simulado devuelve una lista vacía)

        // Act + Assert
        mvc.perform(get("/api/estadisticas/F1/2025/campeonato/pilotos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        verify(estadisticaService).campeonatoPilotos("F1", 2025);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/estadisticas/F1/1949/campeonato/pilotos",
            "/api/estadisticas/F1/2101/resumen",
            "/api/estadisticas/F1/2025/evolucion?top=0",
            "/api/estadisticas/F1/2025/evolucion?top=21",
            "/api/estadisticas/F1/2025/evolucion-grupos?top=21",
            "/api/estadisticas/F1/2025/evolucion-grupos?tipo=PILOTO",
            "/api/estadisticas/F1/2025/comparativa?a=1",
            "/api/estadisticas/F1/2025/tendencia-marca",
            "/api/categorias/F1/temporadas/1949/eventos"
    })
    void parametrosFueraDeLosLimites_respondenBadRequestYNoLlegaAlServicio(String ruta) throws Exception {
        // Arrange (año, top, tipo o parámetros obligatorios inválidos)

        // Act + Assert
        mvc.perform(get(ruta))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(estadisticaService, eventoService);
    }

    @Test
    void evolucionDeGrupos_conTipoEnMinusculas_esValido() throws Exception {
        // Arrange (la expresión del tipo no distingue mayúsculas)

        // Act + Assert
        mvc.perform(get("/api/estadisticas/F1/2025/evolucion-grupos?tipo=marca"))
                .andExpect(status().isOk());
        verify(estadisticaService).evolucionGrupos("F1", 2025, "marca", 6);
    }

    @Test
    void temporadaInexistente_respondeNotFoundConElMensajeDelServicio() throws Exception {
        // Arrange
        when(estadisticaService.resumen("F1", 1999))
                .thenThrow(new ResourceNotFoundException("No hay datos de la temporada 1999 de F1"));

        // Act + Assert
        mvc.perform(get("/api/estadisticas/F1/1999/resumen"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void recalcularPalmares_sinToken_respondeUnauthorized() throws Exception {
        // Arrange (pedido sin credenciales)

        // Act + Assert
        mvc.perform(post("/api/estadisticas/F1/palmares/recalcular"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(estadisticaService);
    }

    @Test
    void recalcularPalmares_conUsuarioSinRolAdmin_respondeForbidden() throws Exception {
        // Arrange (usuario autenticado pero sin ROLE_ADMIN)

        // Act + Assert
        mvc.perform(post("/api/estadisticas/F1/palmares/recalcular").with(jwt()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(estadisticaService);
    }

    @Test
    void recalcularPalmares_conAdmin_devuelveLaCategoriaYLaCantidadActualizada() throws Exception {
        // Arrange
        when(estadisticaService.recalcularPalmares("f1")).thenReturn(24);

        // Act + Assert
        mvc.perform(post("/api/estadisticas/f1/palmares/recalcular").with(jwt().authorities(ROL_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("F1"))
                .andExpect(jsonPath("$.pilotosActualizados").value(24));
    }

    @Test
    void detalleDeEvento_conZonaDelPedido_laResuelveYSeLaPasaAlServicio() throws Exception {
        // Arrange (el servicio simulado devuelve null: alcanza para comprobar la zona recibida)

        // Act
        mvc.perform(get("/api/eventos/9").param("zona", "Europe/Madrid"))
                .andExpect(status().isOk());

        // Assert
        verify(eventoService).detalle(9L, ZoneId.of("Europe/Madrid"));
    }

    @Test
    void detalleDeEvento_conZonaInvalida_respondeBadRequest() throws Exception {
        // Arrange (zona que no existe en la base IANA)

        // Act + Assert
        mvc.perform(get("/api/eventos/9").param("zona", "Marte/Olympus"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(eventoService);
    }

    @Test
    void detalleDeEvento_sinZonaYSinSesion_usaUtc() throws Exception {
        // Arrange (pedido anónimo y sin parámetro zona)

        // Act
        mvc.perform(get("/api/eventos/9")).andExpect(status().isOk());

        // Assert
        verify(eventoService).detalle(9L, ZoneId.of("UTC"));
    }
}

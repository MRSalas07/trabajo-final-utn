package com.tfi.motorsport.backend.controllers.gestion;

import com.tfi.motorsport.backend.services.gestion.ICategoriaGestionService;
import com.tfi.motorsport.backend.services.gestion.IEquipoGestionService;
import com.tfi.motorsport.backend.services.gestion.IEtapaGestionService;
import com.tfi.motorsport.backend.services.gestion.IEventoGestionService;
import com.tfi.motorsport.backend.services.gestion.IPalmaresGestionService;
import com.tfi.motorsport.backend.services.gestion.IParticipacionGestionService;
import com.tfi.motorsport.backend.services.gestion.IPilotoGestionService;
import com.tfi.motorsport.backend.services.gestion.IResultadoEtapaGestionService;
import com.tfi.motorsport.backend.services.gestion.IResultadoSesionGestionService;
import com.tfi.motorsport.backend.services.gestion.ISesionGestionService;
import com.tfi.motorsport.backend.services.gestion.ITemporadaGestionService;
import com.tfi.motorsport.backend.services.gestion.ITripulacionGestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rutas de los controladores de gestión con los servicios simulados (@MockitoBean) y la seguridad real:
 * códigos HTTP (200, 201, 204), validación de parámetros y que solo el rol ADMIN pueda operar.
 * La lógica de cada ABM se prueba en services/gestion/Impl; la delegación, en cada controlador.
 * Cada prueba sigue el patrón AAA: Arrange (preparar), Act (ejecutar), Assert (verificar).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GestionRutasTest {

    private static final SimpleGrantedAuthority ROL_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ICategoriaGestionService categorias;
    @MockitoBean
    private IPalmaresGestionService palmares;
    @MockitoBean
    private IEtapaGestionService etapas;
    @MockitoBean
    private IEventoGestionService eventos;
    @MockitoBean
    private ISesionGestionService sesiones;
    @MockitoBean
    private ITemporadaGestionService temporadas;
    @MockitoBean
    private IResultadoEtapaGestionService resultadosEtapa;
    @MockitoBean
    private IResultadoSesionGestionService resultadosSesion;
    @MockitoBean
    private IParticipacionGestionService participaciones;
    @MockitoBean
    private ITripulacionGestionService tripulaciones;
    @MockitoBean
    private IPilotoGestionService pilotos;
    @MockitoBean
    private IEquipoGestionService equipos;

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/admin/categorias",
            "/api/admin/palmares?categoriaId=1",
            "/api/admin/etapas?eventoId=1",
            "/api/admin/eventos?temporadaId=1",
            "/api/admin/sesiones?eventoId=1",
            "/api/admin/temporadas?categoriaId=1",
            "/api/admin/resultados-etapa?etapaId=1",
            "/api/admin/resultados-sesion?sesionId=1",
            "/api/admin/participaciones?categoriaId=1&anio=2025",
            "/api/admin/tripulaciones?categoriaId=1",
            "/api/admin/pilotos?q=alo",
            "/api/admin/equipos"
    })
    void listados_conAdmin_respondenConUnaLista(String ruta) throws Exception {
        // Arrange (los servicios simulados devuelven listas vacías)

        // Act + Assert
        mvc.perform(get(ruta).with(jwt().authorities(ROL_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/admin/etapas/1",
            "/api/admin/eventos/1",
            "/api/admin/sesiones/1",
            "/api/admin/temporadas/1",
            "/api/admin/resultados-etapa/1",
            "/api/admin/resultados-sesion/1",
            "/api/admin/participaciones/1",
            "/api/admin/tripulaciones/1",
            "/api/admin/pilotos/1",
            "/api/admin/equipos/1"
    })
    void bajas_conAdmin_respondenSinContenido(String ruta) throws Exception {
        // Arrange (la baja es lógica: el servicio simulado no devuelve nada)

        // Act + Assert
        mvc.perform(delete(ruta).with(jwt().authorities(ROL_ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    void baja_conElTokenDelAdministrador_registraSuIdParaLaAuditoria() throws Exception {
        // Arrange
        var admin = jwt().jwt(token -> token.subject("7")).authorities(ROL_ADMIN);

        // Act
        mvc.perform(delete("/api/admin/pilotos/15").with(admin)).andExpect(status().isNoContent());

        // Assert
        verify(pilotos).eliminar(15L, 7L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/admin/pilotos/1", "/api/admin/equipos/1", "/api/admin/eventos/1"})
    void bajas_conUsuarioSinRolAdmin_respondenForbiddenYNoLlegaAlServicio(String ruta) throws Exception {
        // Arrange (usuario autenticado pero sin ROLE_ADMIN)

        // Act + Assert
        mvc.perform(delete(ruta).with(jwt()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(pilotos, equipos, eventos);
    }

    @Test
    void alta_deUnPilotoValido_respondeCreatedYPasaElIdDelAdministrador() throws Exception {
        // Arrange
        var admin = jwt().jwt(token -> token.subject("7")).authorities(ROL_ADMIN);

        // Act
        mvc.perform(post("/api/admin/pilotos").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Franco\",\"apellido\":\"Colapinto\",\"codigo\":\"COL\",\"numero\":43}"))
                .andExpect(status().isCreated());

        // Assert
        verify(pilotos).crear(any(), eq(7L));
    }

    @Test
    void modificacion_deUnEquipoValido_respondeOkYPasaElIdDelRegistro() throws Exception {
        // Arrange
        var admin = jwt().jwt(token -> token.subject("7")).authorities(ROL_ADMIN);

        // Act
        mvc.perform(put("/api/admin/equipos/4").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Alpine\",\"nombreCorto\":\"Alpine\"}"))
                .andExpect(status().isOk());

        // Assert
        verify(equipos).modificar(eq(4L), any(), eq(7L));
    }

    @Test
    void altaDeUnEquipoSinNombre_respondeBadRequestYNoLlegaAlServicio() throws Exception {
        // Arrange
        String cuerpoSinNombre = "{\"nombreCorto\":\"Alpine\"}";

        // Act + Assert
        mvc.perform(post("/api/admin/equipos").with(jwt().authorities(ROL_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoSinNombre))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validación"));
        verifyNoInteractions(equipos);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/admin/participaciones?categoriaId=1&anio=1949",
            "/api/admin/tripulaciones?categoriaId=1&anio=2101",
            "/api/admin/pilotos?limit=501",
            "/api/admin/equipos?limit=0"
    })
    void listados_conParametrosFueraDeRango_respondenBadRequest(String ruta) throws Exception {
        // Arrange (parámetros fuera de los límites declarados en el controlador)

        // Act + Assert
        mvc.perform(get(ruta).with(jwt().authorities(ROL_ADMIN)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(participaciones, tripulaciones, pilotos, equipos);
    }
}

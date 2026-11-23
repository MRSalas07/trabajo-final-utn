package com.tfi.motorsport.backend.controllers.gestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoints de gestión (MVP 5) contra la base H2 de pruebas: reglas de acceso, validación de los
 * formularios y 404 de registros inexistentes. Las reglas de negocio de cada ABM se prueban en
 * services/gestion/Impl, con los repositorios simulados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GestionDatosControllerTest {

    private static final SimpleGrantedAuthority ROL_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

    @Autowired
    private MockMvc mvc;

    @Test
    void sinToken_devuelve401() throws Exception {
        mvc.perform(get("/api/admin/categorias"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("No autenticado"));
    }

    @Test
    void usuarioSinRolAdmin_devuelve403AunqueSeaUnaConsulta() throws Exception {
        mvc.perform(get("/api/admin/temporadas").param("categoriaId", "1").with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado"));
    }

    @Test
    void listadoDeCategoriasDelPanel_conAdmin_devuelveUnaLista() throws Exception {
        mvc.perform(get("/api/admin/categorias").with(jwt().authorities(ROL_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listadoDeTemporadas_sinCategoria_devuelve400() throws Exception {
        mvc.perform(get("/api/admin/temporadas").with(jwt().authorities(ROL_ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Parámetro faltante"));
    }

    @Test
    void altaConDatosInvalidos_devuelve400ConLosErroresDeValidacion() throws Exception {
        mvc.perform(post("/api/admin/pilotos").with(jwt().authorities(ROL_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Franco\",\"apellido\":\"\",\"codigo\":\"COL43\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validación"));
    }

    @Test
    void modificarUnaTemporadaInexistente_devuelve404() throws Exception {
        mvc.perform(put("/api/admin/temporadas/999999").with(jwt().authorities(ROL_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoriaId\":1,\"anio\":2026}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void bajaDeUnEventoInexistente_devuelve404() throws Exception {
        mvc.perform(delete("/api/admin/eventos/999999").with(jwt().authorities(ROL_ADMIN)))
                .andExpect(status().isNotFound());
    }
}

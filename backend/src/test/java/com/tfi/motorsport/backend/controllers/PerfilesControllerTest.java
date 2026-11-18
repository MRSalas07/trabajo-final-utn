package com.tfi.motorsport.backend.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoints de pilotos y equipos contra la base H2 de pruebas (vacía).
 * Levantar el contexto también valida las consultas JPQL nuevas: si una tiene un error, falla al iniciar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PerfilesControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void listadoDePilotos_sinFiltros_devuelvePaginaVaciaConValoresPorDefecto() throws Exception {
        mvc.perform(get("/api/pilotos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isArray())
                .andExpect(jsonPath("$.contenido").isEmpty())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanio").value(24))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void listadoDePilotos_conTextoAnioYPaginado_ejecutaLaBusqueda() throws Exception {
        mvc.perform(get("/api/pilotos")
                        .param("filPiloto", "ham")
                        .param("filtroAnio", "2025")
                        .param("paginado", "1")
                        .param("cantidad", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamanio").value(10));
    }

    @Test
    void listadoDePilotos_cantidadFueraDeRango_devuelve400() throws Exception {
        mvc.perform(get("/api/pilotos").param("cantidad", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/pilotos").param("cantidad", "0")).andExpect(status().isBadRequest());
    }

    @Test
    void listadoDePilotos_paginaNegativaOAnioInvalido_devuelve400() throws Exception {
        mvc.perform(get("/api/pilotos").param("paginado", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/pilotos").param("filtroAnio", "1800")).andExpect(status().isBadRequest());
    }

    @Test
    void listadoDePilotos_categoriaInexistente_devuelve404() throws Exception {
        mvc.perform(get("/api/pilotos").param("filtroCategoria", "XX"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void fichaDePiloto_inexistente_devuelve404() throws Exception {
        mvc.perform(get("/api/pilotos/999")).andExpect(status().isNotFound());
    }

    @Test
    void listadoDeEquipos_sinCategoria_devuelve400() throws Exception {
        mvc.perform(get("/api/equipos")).andExpect(status().isBadRequest());
    }

    @Test
    void listadoDeEquipos_categoriaInexistente_devuelve404() throws Exception {
        mvc.perform(get("/api/equipos").param("categoria", "XX")).andExpect(status().isNotFound());
    }

    @Test
    void fichaDeEquipo_inexistente_devuelve404() throws Exception {
        mvc.perform(get("/api/equipos/999")).andExpect(status().isNotFound());
    }
}

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

/** Endpoints del calendario contra la base H2 de pruebas (vacía). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CalendarioControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void calendario_conRangoValido_devuelveListaVacia() throws Exception {
        mvc.perform(get("/api/calendario")
                        .param("desde", "2025-03-01")
                        .param("hasta", "2025-03-31")
                        .param("zona", "America/Argentina/Buenos_Aires"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void calendario_sinFechasOConFechaInvalida_devuelve400() throws Exception {
        mvc.perform(get("/api/calendario").param("hasta", "2025-03-31")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/calendario").param("desde", "2025-13-01").param("hasta", "2025-03-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calendario_hastaAnteriorADesde_devuelve400() throws Exception {
        mvc.perform(get("/api/calendario").param("desde", "2025-03-31").param("hasta", "2025-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void proximosYRecientes_devuelvenListas() throws Exception {
        mvc.perform(get("/api/calendario/proximos").param("limite", "4")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        mvc.perform(get("/api/calendario/recientes")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void proximos_limiteFueraDeRango_devuelve400() throws Exception {
        mvc.perform(get("/api/calendario/proximos").param("limite", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/calendario/recientes").param("limite", "21")).andExpect(status().isBadRequest());
    }

    @Test
    void icsDeEventoInexistente_devuelve404() throws Exception {
        mvc.perform(get("/api/calendario/eventos/999.ics")).andExpect(status().isNotFound());
    }

    @Test
    void icsDeEventoConIdInvalido_devuelve400() throws Exception {
        mvc.perform(get("/api/calendario/eventos/abc.ics")).andExpect(status().isBadRequest());
    }

    @Test
    void icsDeTemporadaInexistente_devuelve404() throws Exception {
        mvc.perform(get("/api/calendario/F1/2025.ics")).andExpect(status().isNotFound());
    }
}

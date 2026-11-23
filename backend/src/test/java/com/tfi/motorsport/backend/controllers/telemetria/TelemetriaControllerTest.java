package com.tfi.motorsport.backend.controllers.telemetria;

import com.tfi.motorsport.backend.clients.openf1.OpenF1Cliente;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Módulo de telemetría HABILITADO (con OpenF1 simulado): sus endpoints existen, son públicos,
 * validan los parámetros y /api/salud lo informa.
 */
@SpringBootTest(properties = "app.telemetria.habilitada=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TelemetriaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OpenF1Cliente openF1;

    @Test
    void saludInformaQueLaTelemetriaEstaHabilitada() throws Exception {
        mvc.perform(get("/api/salud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telemetria").value(true));
    }

    @Test
    void sesiones_anioAnteriorA2023_devuelve400SinConsultarOpenF1() throws Exception {
        mvc.perform(get("/api/telemetria/sesiones").param("anio", "2022"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(openF1);
    }

    @Test
    void sesionInexistenteEnOpenF1_devuelve404() throws Exception {
        when(openF1.get("sessions?session_key=1")).thenReturn(JsonMapper.builder().build().readTree("[]"));

        mvc.perform(get("/api/telemetria/sesiones/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}

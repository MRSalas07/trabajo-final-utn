package com.tfi.motorsport.backend.controllers.telemetria;

import com.tfi.motorsport.backend.clients.openf1.OpenF1Cliente;
import com.tfi.motorsport.backend.services.telemetria.ITelemetriaService;
import com.tfi.motorsport.backend.services.telemetria.ITransmisionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Módulo de telemetría APAGADO (perfil test: app.telemetria.habilitada=false): sus clases no se crean,
 * sus rutas no existen y el resto del sistema funciona igual.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TelemetriaDeshabilitadaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationContext contexto;

    @Test
    void noSeCreanLasClasesDelModulo() {
        assertThat(contexto.getBeanNamesForType(OpenF1Cliente.class)).isEmpty();
        assertThat(contexto.getBeanNamesForType(ITelemetriaService.class)).isEmpty();
        assertThat(contexto.getBeanNamesForType(ITransmisionService.class)).isEmpty();
    }

    @Test
    void saludLoInformaYLasRutasNoExisten() throws Exception {
        mvc.perform(get("/api/salud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telemetria").value(false));
        mvc.perform(get("/api/telemetria/sesiones").param("anio", "2025"))
                .andExpect(status().isNotFound());
    }
}

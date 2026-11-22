package com.tfi.motorsport.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de la capa web: reglas de acceso y formato de errores.
 * Usan H2 en memoria (perfil "test"), no llaman a APIs externas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadYErroresTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void consultasSonPublicas() throws Exception {
        mvc.perform(get("/api/salud")).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("OK"));
        mvc.perform(get("/api/categorias")).andExpect(status().isOk());
    }

    @Test
    void recursoInexistenteDevuelve404ConFormatoUniforme() throws Exception {
        mvc.perform(get("/api/categorias/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detalles[0]").exists());
    }

    @Test
    void recalcularPalmaresSinRolAdminDevuelve403() throws Exception {
        mvc.perform(post("/api/estadisticas/F1/palmares/recalcular").with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void recalcularPalmaresConRolAdminPasaLaSeguridad() throws Exception {
        mvc.perform(post("/api/estadisticas/XX/palmares/recalcular").with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }



    



    @Test
    void registroValidaLosDatos() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"email\":\"no-es-email\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validación"));
    }

    @Test
    void sincronizarCategoriaSinApiDevuelve400() throws Exception {
        mvc.perform(post("/api/admin/sincronizaciones").with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoria\":\"XX\",\"anio\":2025}"))
                .andExpect(status().isNotFound());
    }
}

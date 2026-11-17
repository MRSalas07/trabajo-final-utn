package com.tfi.motorsport.backend.handlers;

import com.tfi.motorsport.backend.dtos.error.ErrorDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del manejo de errores: cada error esperable responde con su código HTTP
 * y el formato uniforme {@link ErrorDto}, nunca con un 500 ni con detalles internos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void parametroFueraDeRango_devuelve400() throws Exception {
        mvc.perform(get("/api/categorias/F1/temporadas/1800/eventos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Error de validación"))
                .andExpect(jsonPath("$.detalles[0]").exists());
    }

    @Test
    void parametroDeTipoIncorrecto_devuelve400() throws Exception {
        mvc.perform(get("/api/eventos/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Parámetro inválido"));
    }

    @Test
    void metodoNoSoportado_devuelve405() throws Exception {
        mvc.perform(post("/api/salud"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void registroConContrasenaDeMasDe72Caracteres_devuelve400() throws Exception {
        String password = "a".repeat(73);
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"email\":\"ana@test.local\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validación"));
    }

    @Test
    void errorNoControlado_devuelve500SinExponerElMensajeInterno() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ErrorDto> respuesta = handler.handleGeneric(
                new IllegalStateException("detalle interno: tabla evento, columna x"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().detalles()).noneMatch(d -> d.contains("detalle interno"));
    }

    @Test
    void violacionDeIntegridad_devuelve409SinExponerLaBase() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ErrorDto> respuesta = handler.handleDataIntegrity(
                new DataIntegrityViolationException("duplicate key value violates unique constraint \"ux_usuario_email\""));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().detalles()).noneMatch(d -> d.contains("ux_usuario_email"));
    }
}

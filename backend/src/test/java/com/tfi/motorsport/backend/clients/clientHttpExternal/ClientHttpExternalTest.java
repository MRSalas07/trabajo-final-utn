package com.tfi.motorsport.backend.clients.clientHttpExternal;

import com.sun.net.httpserver.HttpServer;
import com.tfi.motorsport.backend.handlers.exceptions.IntegrationExternalException;
import com.tfi.motorsport.backend.handlers.exceptions.IntegrationExternalException.Tipo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientHttpExternalTest {

    private record Respuesta(int codigo, String cuerpo) {
    }

    private HttpServer servidor;
    private final Deque<Respuesta> respuestas = new ArrayDeque<>();
    private final AtomicInteger llamadas = new AtomicInteger();

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            llamadas.incrementAndGet();
            Respuesta respuesta = respuestas.size() > 1 ? respuestas.poll() : respuestas.peek();
            byte[] cuerpo = respuesta.cuerpo().getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Retry-After", "0");
            intercambio.sendResponseHeaders(respuesta.codigo(), cuerpo.length == 0 ? -1 : cuerpo.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(cuerpo);
            }
        });
        servidor.start();
    }

    @AfterEach
    void detenerServidor() {
        servidor.stop(0);
    }

    private ClientHttpExternal cliente(int reintentos) {
        String urlBase = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/";
        return new ClientHttpExternal(urlBase, JsonMapper.builder().build(), "Prueba", 2000, 2000, reintentos, 0);
    }

    private void responder(int codigo, String cuerpo) {
        respuestas.add(new Respuesta(codigo, cuerpo));
    }

    @Test
    void respuestaCorrecta_devuelveElJson() {
        responder(200, "{\"temporada\":2025}");

        JsonNode json = cliente(2).get("f1/2025.json");

        assertThat(json.get("temporada").asInt()).isEqualTo(2025);
        assertThat(llamadas.get()).isEqualTo(1);
    }

    @Test
    void errorTemporalDelProveedor_reintentaYLuegoResponde() {
        responder(503, "");
        responder(429, "");
        responder(200, "{\"ok\":true}");

        JsonNode json = cliente(2).get("f1/2025.json");

        assertThat(json.get("ok").asBoolean()).isTrue();
        assertThat(llamadas.get()).isEqualTo(3);
    }

    @Test
    void limiteDeUsoSinReintentosDisponibles_lanzaLimiteUso() {
        responder(429, "");

        assertThatThrownBy(() -> cliente(1).get("f1/2025.json"))
                .isInstanceOf(IntegrationExternalException.class)
                .extracting(ex -> ((IntegrationExternalException) ex).getTipo())
                .isEqualTo(Tipo.LIMITE_USO);
        assertThat(llamadas.get()).isEqualTo(2);
    }

    @Test
    void proveedorCaido_lanzaNoDisponibleDespuesDeLosReintentos() {
        responder(500, "");

        assertThatThrownBy(() -> cliente(2).get("f1/2025.json"))
                .isInstanceOf(IntegrationExternalException.class)
                .extracting(ex -> ((IntegrationExternalException) ex).getTipo())
                .isEqualTo(Tipo.NO_DISPONIBLE);
        assertThat(llamadas.get()).isEqualTo(3);
    }

    @Test
    void recursoInexistente_noReintenta() {
        responder(404, "");

        assertThatThrownBy(() -> cliente(3).get("f1/1800.json"))
                .isInstanceOf(IntegrationExternalException.class)
                .extracting(ex -> ((IntegrationExternalException) ex).getTipo())
                .isEqualTo(Tipo.ERROR_PROVEEDOR);
        assertThat(llamadas.get()).isEqualTo(1);
    }

    @Test
    void respuestaQueNoEsJson_lanzaFormatoInvalido() {
        responder(200, "<html>mantenimiento</html>");

        assertThatThrownBy(() -> cliente(0).get("f1/2025.json"))
                .isInstanceOf(IntegrationExternalException.class)
                .extracting(ex -> ((IntegrationExternalException) ex).getTipo())
                .isEqualTo(Tipo.FORMATO_INVALIDO);
    }

    @Test
    void respuestaVacia_lanzaRespuestaVacia() {
        responder(200, "");

        assertThatThrownBy(() -> cliente(0).get("f1/2025.json"))
                .isInstanceOf(IntegrationExternalException.class)
                .extracting(ex -> ((IntegrationExternalException) ex).getTipo())
                .isEqualTo(Tipo.RESPUESTA_VACIA);
    }
}

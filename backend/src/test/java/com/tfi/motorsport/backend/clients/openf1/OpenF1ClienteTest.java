package com.tfi.motorsport.backend.clients.openf1;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.tfi.motorsport.backend.config.AppProperties;
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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cliente de OpenF1 contra un servidor HTTP local que imita sus respuestas: consultas sin resultados (404),
 * restricción durante una sesión en vivo (401) y el inicio de sesión de la suscripción.
 */
class OpenF1ClienteTest {

    private HttpServer servidor;
    private final AtomicInteger inicios = new AtomicInteger();
    private final List<String> autorizaciones = new CopyOnWriteArrayList<>();

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/token", intercambio -> {
            inicios.incrementAndGet();
            String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            boolean valido = cuerpo.contains("username=fan%40mail.com") && cuerpo.contains("password=secreta");
            responder(intercambio, valido ? 200 : 401, valido
                    ? "{\"access_token\": \"tok-123\", \"expires_in\": \"3600\", \"token_type\": \"bearer\"}"
                    : "{\"detail\": \"Invalid credentials\"}");
        });
        servidor.createContext("/v1/", intercambio -> {
            autorizaciones.add(String.valueOf(intercambio.getRequestHeaders().getFirst("Authorization")));
            String ruta = intercambio.getRequestURI().toString();
            if (ruta.contains("sin_resultados")) {
                responder(intercambio, 404, "{\"detail\": \"No results found.\"}");
            } else if (ruta.contains("restringido")) {
                responder(intercambio, 401, "{\"detail\": \"Live F1 session in progress\"}");
            } else {
                responder(intercambio, 200, "[{\"session_key\": 9839}]");
            }
        });
        servidor.start();
    }

    @AfterEach
    void detenerServidor() {
        servidor.stop(0);
    }

    private static void responder(HttpExchange intercambio, int codigo, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().add("Content-Type", "application/json");
        intercambio.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(bytes);
        }
    }

    private OpenF1Cliente cliente(String usuario, String password) {
        String base = "http://127.0.0.1:" + servidor.getAddress().getPort();
        AppProperties.Telemetria telemetria = new AppProperties.Telemetria(true, base + "/v1/", 0, 100,
                base + "/token", usuario, password, 100);
        AppProperties.Integracion integracion = new AppProperties.Integracion(2000, 2000, 0, null, null, false, null);
        return new OpenF1Cliente(new AppProperties(null, null, null, integracion, telemetria),
                JsonMapper.builder().build());
    }

    @Test
    void consultaSinResultados_devuelveUnaListaVacia() {
        JsonNode respuesta = cliente(null, null).get("laps?session_key=sin_resultados");

        assertThat(respuesta.isArray()).isTrue();
        assertThat(respuesta.size()).isZero();
    }

    @Test
    void consultaPublicaRechazada_explicaLaRestriccionDeOpenF1() {
        assertThatThrownBy(() -> cliente(null, null).get("position?session_key=restringido"))
                .isInstanceOfSatisfying(IntegrationExternalException.class,
                        ex -> assertThat(ex.getTipo()).isEqualTo(Tipo.NO_AUTORIZADO))
                .hasMessageContaining("solo para suscriptores");
    }

    @Test
    void vivo_sinCredenciales_noConsultaNada() {
        assertThatThrownBy(() -> cliente("", "").getVivo("sessions?session_key=latest"))
                .isInstanceOf(IntegrationExternalException.class)
                .hasMessageContaining("OPENF1_USUARIO");
        assertThat(inicios.get()).isZero();
        assertThat(autorizaciones).isEmpty();
    }

    @Test
    void vivo_iniciaSesionUnaSolaVezYMandaElToken() {
        OpenF1Cliente cliente = cliente("fan@mail.com", "secreta");

        JsonNode respuesta = cliente.getVivo("sessions?session_key=latest");
        cliente.getVivo("drivers?session_key=9839");

        assertThat(respuesta.get(0).get("session_key").asInt()).isEqualTo(9839);
        assertThat(inicios.get()).isEqualTo(1);
        assertThat(autorizaciones).containsExactly("Bearer tok-123", "Bearer tok-123");
    }

    @Test
    void vivo_credencialesInvalidas_lanzaNoAutorizado() {
        assertThatThrownBy(() -> cliente("fan@mail.com", "otra").getVivo("sessions?session_key=latest"))
                .isInstanceOfSatisfying(IntegrationExternalException.class,
                        ex -> assertThat(ex.getTipo()).isEqualTo(Tipo.NO_AUTORIZADO))
                .hasMessageContaining("No se pudo iniciar sesión");
        assertThat(autorizaciones).isEmpty();
    }
}

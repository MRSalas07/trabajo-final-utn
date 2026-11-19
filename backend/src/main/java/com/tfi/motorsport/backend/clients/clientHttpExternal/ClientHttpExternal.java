package com.tfi.motorsport.backend.clients.clientHttpExternal;

import com.tfi.motorsport.backend.handlers.exceptions.IntegrationExternalException;
import com.tfi.motorsport.backend.handlers.exceptions.IntegrationExternalException.Tipo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Duration;

@Slf4j
public class ClientHttpExternal {

    private final RestClient restClient;

    private final JsonMapper jsonMapper;

    private static final long ESPERA_BASE_MS = 1000;
    private static final long ESPERA_MAXIMA_MS = 30_000;

    private final String proveedor;
    private final int reintentos;
    private final long intervaloMinimoMs;

    /** Momento de la última llamada (para el throttling). Protegido por "synchronized". */
    private long ultimaLlamadaMs = 0;

    public ClientHttpExternal(String urlBase, JsonMapper jsonMapper, String proveedor,
                              int timeoutConexionMs, int timeoutLecturaMs,
                              int reintentos, long intervaloMinimoMs) {

        this.jsonMapper = jsonMapper;
        this.proveedor = proveedor;
        this.reintentos = Math.max(0, reintentos);
        this.intervaloMinimoMs = Math.max(0, intervaloMinimoMs);

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutConexionMs))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(timeoutLecturaMs));

        this.restClient = RestClient.builder()
                .baseUrl(urlBase)
                .requestFactory(factory)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    public JsonNode get(String ruta) {
        int intento = 0;
        while (true) {
            intento++;
            try {
                esperarTurno();
                String cuerpo = restClient.get().uri(ruta).retrieve().body(String.class);
                return parsear(ruta, cuerpo);
            } catch (RestClientResponseException ex) {
                HttpStatusCode estado = ex.getStatusCode();
                boolean reintentable = estado.value() == 429 || estado.is5xxServerError();
                if (!reintentable || intento > reintentos) {
                    throw traducirErrorHttp(ruta, ex);
                }
                long espera = calcularEspera(intento, ex);
                log.warn("[{}] HTTP {} en '{}'. Reintento {}/{} en {} ms", proveedor, estado.value(), ruta, intento, reintentos, espera);
                dormir(espera);
            } catch (ResourceAccessException ex) {
                if (intento > reintentos) {
                    throw new IntegrationExternalException(proveedor, IntegrationExternalException.Tipo.TIMEOUT,
                            "Sin respuesta del proveedor (timeout o error de red) en '" + ruta + "'", ex);
                }
                dormir(calcularEspera(intento, null));
            }
        }
    }

    /** Throttling: garantiza un intervalo mínimo entre llamadas al mismo proveedor. */
    private synchronized void esperarTurno() {
        long ahora = System.currentTimeMillis();
        long faltante = ultimaLlamadaMs + intervaloMinimoMs - ahora;
        if (faltante > 0) {
            dormir(faltante);
        }
        ultimaLlamadaMs = System.currentTimeMillis();
    }

    private JsonNode parsear(String ruta, String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new IntegrationExternalException(proveedor, Tipo.RESPUESTA_VACIA,
                    "El proveedor devolvió una respuesta vacía en '" + ruta + "'");
        }
        try {
            return jsonMapper.readTree(cuerpo);
        } catch (JacksonException ex) {
            throw new IntegrationExternalException(proveedor, Tipo.FORMATO_INVALIDO,
                    "La respuesta de '" + ruta + "' no es un JSON válido", ex);
        }
    }

    /** Espera exponencial: 1s, 2s, 4s... o lo que indique el header Retry-After (con tope). */
    private long calcularEspera(int intento, RestClientResponseException ex) {
        if (ex != null && ex.getResponseHeaders() != null) {
            String retryAfter = ex.getResponseHeaders().getFirst("Retry-After");
            if (retryAfter != null) {
                try {
                    return Math.min(Long.parseLong(retryAfter.trim()) * 1000, ESPERA_MAXIMA_MS);
                } catch (NumberFormatException ignorada) {
                    // Retry-After también puede venir como fecha HTTP; en ese caso usamos el exponencial
                }
            }
        }
        return Math.min(ESPERA_BASE_MS * (1L << (intento - 1)), ESPERA_MAXIMA_MS);
    }

    private IntegrationExternalException traducirErrorHttp(String ruta, RestClientResponseException ex) {
        int codigo = ex.getStatusCode().value();
        if (codigo == 429) {
            return new IntegrationExternalException(proveedor, Tipo.LIMITE_USO,
                    "Se superó el límite de consultas del proveedor. Intente más tarde.", ex);
        }
        if (codigo == 404) {
            return new IntegrationExternalException(proveedor, Tipo.ERROR_PROVEEDOR,
                    "El recurso '" + ruta + "' no existe en el proveedor (HTTP 404)", ex);
        }
        if (ex.getStatusCode().is5xxServerError()) {
            return new IntegrationExternalException(proveedor, Tipo.NO_DISPONIBLE,
                    "El proveedor no está disponible (HTTP " + codigo + ")", ex);
        }
        return new IntegrationExternalException(proveedor, Tipo.ERROR_PROVEEDOR,
                "El proveedor rechazó la consulta '" + ruta + "' (HTTP " + codigo + ")", ex);
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operación interrumpida", e);
        }
    }


}

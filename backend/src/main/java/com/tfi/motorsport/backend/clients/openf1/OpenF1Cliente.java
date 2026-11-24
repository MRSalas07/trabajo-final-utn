package com.tfi.motorsport.backend.clients.openf1;

import com.tfi.motorsport.backend.clients.clientHttpExternal.ClientHttpExternal;
import com.tfi.motorsport.backend.clients.clientHttpExternal.FormatJson;
import com.tfi.motorsport.backend.config.AppProperties;
import com.tfi.motorsport.backend.handlers.exceptions.IntegrationExternalException;
import com.tfi.motorsport.backend.handlers.exceptions.IntegrationExternalException.Tipo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Cliente de OpenF1 (https://openf1.org), la fuente de la telemetría F1 del MVP 7.
 *
 * Es UNO SOLO para todo el módulo (análisis, foto de la sesión y vivo), así el cupo del proveedor
 * se respeta en conjunto: ClientHttpExternal separa las consultas (límite por segundo, reintentos ante
 * 429/5xx) y acá se suma el límite por minuto con una ventana deslizante.
 *
 *  - Datos históricos: gratis y sin autenticación ({@link #get}).
 *  - Datos en vivo: requieren la suscripción paga ({@link #getVivo}). El token se pide con usuario y
 *    contraseña (POST al endpoint de token, formulario username/password), dura 1 hora y se renueva
 *    solo antes de vencer. Nunca sale del backend.
 *
 * OpenF1 responde 404 cuando una consulta no tiene resultados: acá se convierte en una lista vacía.
 * Solo existe si app.telemetria.habilitada=true.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.telemetria", name = "habilitada", havingValue = "true")
public class OpenF1Cliente {

    public static final String PROVEEDOR = "OPENF1";
    private static final long UN_MINUTO_MS = 60_000;
    /** Las respuestas de OpenF1 pueden pesar varios MB: se da más tiempo de lectura que al resto de las APIs. */
    private static final int TIMEOUT_LECTURA_MINIMO_MS = 60_000;
    /** El token se renueva 2 minutos antes de vencer. */
    private static final long MARGEN_RENOVACION_SEG = 120;
    /** Formato de fecha de los filtros de OpenF1 (ej: date>=2025-12-07T13:10:00.000). */
    private static final DateTimeFormatter FORMATO_FILTRO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

    private final AppProperties.Telemetria cfg;
    private final JsonMapper jsonMapper;
    private final ClientHttpExternal cliente;
    private final RestClient login;
    private final VentanaDeslizante cupoPorMinuto;

    private String token;
    private Instant tokenVence = Instant.EPOCH;

    public OpenF1Cliente(AppProperties props, JsonMapper jsonMapper) {
        this.cfg = props.telemetria();
        this.jsonMapper = jsonMapper;
        AppProperties.Integracion integracion = props.integracion();
        this.cliente = new ClientHttpExternal(cfg.urlBase(), jsonMapper, PROVEEDOR,
                integracion.timeoutConexionMs(), Math.max(integracion.timeoutLecturaMs(), TIMEOUT_LECTURA_MINIMO_MS),
                integracion.reintentos(), cfg.intervaloMs());

        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(integracion.timeoutConexionMs()))
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofMillis(integracion.timeoutLecturaMs()));
        this.login = RestClient.builder().requestFactory(fabrica).build();

        // La suscripción permite más consultas por minuto que el plan gratuito
        this.cupoPorMinuto = new VentanaDeslizante(
                cfg.credencialesVivo() ? cfg.limiteMinutoSuscripcion() : cfg.limiteMinuto(), UN_MINUTO_MS);
    }

    /** Consulta pública de OpenF1 (datos históricos), ej: "sessions?year=2025". */
    public JsonNode get(String ruta) {
        esperarCupoPorMinuto();
        return consultar(ruta, null);
    }

    /** Consulta autenticada (datos en vivo). Si OpenF1 rechaza el token, se pide uno nuevo y se reintenta una vez. */
    public JsonNode getVivo(String ruta) {
        String vigente = tokenVigente(false);
        esperarCupoPorMinuto();
        try {
            return consultar(ruta, vigente);
        } catch (IntegrationExternalException ex) {
            if (ex.getTipo() != Tipo.NO_AUTORIZADO) {
                throw ex;
            }
            String nuevo = tokenVigente(true);
            esperarCupoPorMinuto();
            return consultar(ruta, nuevo);
        }
    }

    /** true si están configurados el usuario y la contraseña de la suscripción (habilitan el modo en vivo). */
    public boolean credencialesConfiguradas() {
        return cfg.credencialesVivo();
    }

    /** Fecha para los filtros de OpenF1, en UTC y con milisegundos. */
    public static String fecha(Instant instante) {
        return FORMATO_FILTRO.format(instante);
    }

    private JsonNode consultar(String ruta, String tokenBearer) {
        try {
            return cliente.get(ruta, tokenBearer);
        } catch (IntegrationExternalException ex) {
            if (ex.getCause() instanceof RestClientResponseException r && r.getStatusCode().value() == 404) {
                return jsonMapper.createArrayNode();
            }
            if (tokenBearer == null && ex.getTipo() == Tipo.NO_AUTORIZADO) {
                throw new IntegrationExternalException(PROVEEDOR, Tipo.NO_AUTORIZADO,
                        "OpenF1 rechazó la consulta sin suscripción: durante una sesión en vivo (desde 30 minutos "
                                + "antes hasta 30 minutos después) sus datos son solo para suscriptores. Probá más tarde.", ex);
            }
            throw ex;
        }
    }

    /** Si en el último minuto ya se usó todo el cupo, espera lo justo hasta que se libere un lugar. */
    private void esperarCupoPorMinuto() {
        long espera;
        while ((espera = cupoPorMinuto.reservar(System.currentTimeMillis())) > 0) {
            log.debug("[{}] Cupo por minuto agotado: se espera {} ms", PROVEEDOR, espera);
            try {
                Thread.sleep(espera);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Consulta a OpenF1 interrumpida", e);
            }
        }
    }

    /** Token de la suscripción: se reutiliza mientras esté vigente y se renueva 2 minutos antes de vencer. */
    private synchronized String tokenVigente(boolean renovar) {
        if (!cfg.credencialesVivo()) {
            throw new IntegrationExternalException(PROVEEDOR, Tipo.NO_AUTORIZADO,
                    "El modo en vivo requiere la suscripción de OpenF1: configurá OPENF1_USUARIO y OPENF1_PASSWORD");
        }
        if (!renovar && token != null && Instant.now().isBefore(tokenVence)) {
            return token;
        }
        esperarCupoPorMinuto();
        try {
            MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
            formulario.add("username", cfg.usuario());
            formulario.add("password", cfg.password());
            String cuerpo = login.post().uri(cfg.tokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formulario)
                    .retrieve()
                    .body(String.class);
            JsonNode respuesta = jsonMapper.readTree(cuerpo == null || cuerpo.isBlank() ? "{}" : cuerpo);
            String nuevo = FormatJson.aString(respuesta, "access_token");
            if (nuevo == null) {
                throw new IntegrationExternalException(PROVEEDOR, Tipo.NO_AUTORIZADO,
                        "OpenF1 no devolvió un token de acceso");
            }
            // expires_in llega como texto ("3600"): aLong también lo interpreta
            Long expira = FormatJson.aLong(respuesta, "expires_in");
            token = nuevo;
            tokenVence = Instant.now().plusSeconds(Math.max(60, (expira != null ? expira : 3600) - MARGEN_RENOVACION_SEG));
            log.info("[{}] Token de acceso obtenido (se renueva a las {})", PROVEEDOR, tokenVence);
            return token;
        } catch (RestClientException | JacksonException ex) {
            throw new IntegrationExternalException(PROVEEDOR, Tipo.NO_AUTORIZADO,
                    "No se pudo iniciar sesión en OpenF1: revisá OPENF1_USUARIO y OPENF1_PASSWORD", ex);
        }
    }
}

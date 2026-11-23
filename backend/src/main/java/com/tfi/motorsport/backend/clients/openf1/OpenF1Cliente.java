package com.tfi.motorsport.backend.clients.openf1;

import com.tfi.motorsport.backend.clients.clientHttpExternal.ClientHttpExternal;
import com.tfi.motorsport.backend.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cliente de OpenF1 (https://openf1.org), la fuente de la telemetría F1 del MVP 7.
 *
 * Es UNO SOLO para todo el módulo (análisis de sesiones, repetición y vivo), así el cupo del proveedor
 * se respeta en conjunto: ClientHttpExternal separa las consultas (límite por segundo, reintentos ante
 * 429/5xx) y acá se suma el límite por minuto con una ventana deslizante.
 *
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

    private final ClientHttpExternal cliente;
    private final VentanaDeslizante cupoPorMinuto;

    public OpenF1Cliente(AppProperties props, JsonMapper jsonMapper) {
        AppProperties.Telemetria cfg = props.telemetria();
        AppProperties.Integracion integracion = props.integracion();
        this.cliente = new ClientHttpExternal(cfg.urlBase(), jsonMapper, PROVEEDOR,
                integracion.timeoutConexionMs(), Math.max(integracion.timeoutLecturaMs(), TIMEOUT_LECTURA_MINIMO_MS),
                integracion.reintentos(), cfg.intervaloMs());
        this.cupoPorMinuto = new VentanaDeslizante(cfg.limiteMinuto(), UN_MINUTO_MS);
    }

    /** Consulta pública de OpenF1 (datos históricos), ej: "sessions?year=2025". */
    public JsonNode get(String ruta) {
        esperarCupoPorMinuto();
        return cliente.get(ruta);
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
}

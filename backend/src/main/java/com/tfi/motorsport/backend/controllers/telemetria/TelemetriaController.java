package com.tfi.motorsport.backend.controllers.telemetria;

import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.FotoSesion;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.SesionTelemetria;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.TelemetriaSesion;
import com.tfi.motorsport.backend.services.telemetria.ITelemetriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Telemetría F1 (MVP 7, módulo plus, datos de OpenF1). Consultas públicas como el resto de los GET.
 * Solo existe si TELEMETRIA_HABILITADA=true: si no, estas rutas responden 404 y /api/salud lo informa.
 */
@Tag(name = "Telemetría F1 (plus)")
@RestController
@RequestMapping("/api/telemetria")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.telemetria", name = "habilitada", havingValue = "true")
public class TelemetriaController {

    private final ITelemetriaService telemetria;

    @Operation(summary = "Sesiones de F1 de un año con datos en OpenF1 (desde 2023), de la más reciente a la más vieja")
    @GetMapping("/sesiones")
    public List<SesionTelemetria> sesiones(@RequestParam @Min(2023) @Max(2100) int anio) {
        return telemetria.sesiones(anio);
    }

    @Operation(summary = "Análisis de una sesión terminada: tiempos por vuelta (hasta 6 pilotos) y evolución de posiciones")
    @GetMapping("/sesiones/{sessionKey}")
    public TelemetriaSesion sesion(@PathVariable @Min(1) int sessionKey,
                                   @RequestParam(required = false) List<Integer> pilotos) {
        return telemetria.sesion(sessionKey, pilotos);
    }

    @Operation(summary = "Foto de la sesión: en curso = ahora; terminada = al final de la vuelta pedida, en el "
            + "instante pedido (ISO 8601) o, sin ninguno, el final. pilotos: hasta 2 para el versus")
    @GetMapping("/sesiones/{sessionKey}/foto")
    public FotoSesion foto(@PathVariable @Min(1) int sessionKey,
                           @RequestParam(required = false) @Min(1) @Max(200) Integer vuelta,
                           @RequestParam(required = false) Instant instante,
                           @RequestParam(required = false) List<Integer> pilotos) {
        return telemetria.foto(sessionKey, vuelta, instante, pilotos);
    }
}

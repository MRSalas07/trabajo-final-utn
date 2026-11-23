package com.tfi.motorsport.backend.controllers.telemetria;

import com.tfi.motorsport.backend.dtos.telemetria.TransmisionDtos.InfoVivo;
import com.tfi.motorsport.backend.services.telemetria.ITransmisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Transmisión de telemetría F1 por SSE (Server-Sent Events), MVP 7.
 * El navegador abre la conexión con EventSource y recibe un evento "estado" por segundo (o un evento
 * "error" con el motivo si no se puede transmitir). Cada espectador tiene un id (UUID) que viene en
 * cada foto y sirve para sus controles.
 *
 *  GET /api/telemetria/transmisiones/repeticion?sessionKey=9839&velocidad=5&inicioMin=0&piloto=1
 *  GET /api/telemetria/transmisiones/repeticion?sessionKey=9839&inicioVuelta=20
 *  GET /api/telemetria/transmisiones/vivo?sessionKey=latest&piloto=1   (requiere suscripción de OpenF1)
 *  PUT /api/telemetria/transmisiones/{id}?piloto=44&velocidad=2&pausa=false
 *  GET /api/telemetria/vivo/estado
 */
@Tag(name = "Telemetría F1 (plus)")
@RestController
@RequestMapping("/api/telemetria")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.telemetria", name = "habilitada", havingValue = "true")
public class TransmisionController {

    private final ITransmisionService transmisiones;

    @Operation(summary = "Repetición de una sesión terminada como si fuera en vivo (SSE, datos gratuitos)")
    @GetMapping(value = "/transmisiones/repeticion", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter repeticion(@RequestParam int sessionKey,
                                 @RequestParam(defaultValue = "1") double velocidad,
                                 @RequestParam(defaultValue = "0") int inicioMin,
                                 @RequestParam(required = false) Integer inicioVuelta,
                                 @RequestParam(required = false) Integer piloto) {
        return transmisiones.repeticion(sessionKey, velocidad, inicioMin, inicioVuelta, piloto);
    }

    @Operation(summary = "Transmisión EN VIVO de la sesión en curso (SSE, requiere la suscripción de OpenF1)")
    @GetMapping(value = "/transmisiones/vivo", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter vivo(@RequestParam(defaultValue = "latest") String sessionKey,
                           @RequestParam(required = false) Integer piloto) {
        return transmisiones.vivo(sessionKey, piloto);
    }

    @Operation(summary = "Cambiar el piloto seguido (0 = el líder), la velocidad o pausar una transmisión propia")
    @PutMapping("/transmisiones/{id}")
    public ResponseEntity<Void> ajustar(@PathVariable String id,
                                        @RequestParam(required = false) Integer piloto,
                                        @RequestParam(required = false) Double velocidad,
                                        @RequestParam(required = false) Boolean pausa) {
        transmisiones.ajustar(id, piloto, velocidad, pausa);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "¿Está configurado el modo en vivo? ¿Hay una sesión en curso?")
    @GetMapping("/vivo/estado")
    public InfoVivo estadoVivo() {
        return transmisiones.infoVivo();
    }
}

package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.config.AppProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Estado del servicio. Lo usan:
 *  - el PaaS (health check para saber si la aplicación levantó);
 *  - el frontend, para saber qué módulos opcionales mostrar (ej: telemetría).
 */
@Tag(name = "Salud")
@RestController
@RequiredArgsConstructor
public class SaludController {

    private final AppProperties props;

    public record Salud(String estado, Instant hora) {
    }

    @Operation(summary = "Estado del servicio y módulos habilitados")
    @GetMapping("/api/salud")
    public Salud salud() {
        return new Salud("OK", Instant.now());
    }
}

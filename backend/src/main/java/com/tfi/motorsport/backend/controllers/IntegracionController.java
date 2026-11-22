package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.*;
import com.tfi.motorsport.backend.dtos.integracionAPI.CargaSincronizacionDto;
import com.tfi.motorsport.backend.dtos.integracionAPI.CargaSincronizacionDto.*;
import com.tfi.motorsport.backend.dtos.integracionAPI.ResumenSincronizacion;
import com.tfi.motorsport.backend.services.IEstadisticaService;
import com.tfi.motorsport.backend.services.ISincronizacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Integracion (ADMIN)")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class IntegracionController {

    private final ISincronizacionService sincronizacion;

    private final IEstadisticaService estadisticas;

    @Operation(summary = "Fuentes automáticas disponibles por categoría")
    @GetMapping("/fuentes")
    public List<ISincronizacionService.FuenteDisponible> fuentes() {
        return sincronizacion.fuentes();
    }

    @Operation(summary = "Iniciar la sincronización de una temporada desde la API externa (en segundo plano)")
    @PostMapping("/sincronizaciones")
    public ResponseEntity<CargaSincronizacionDto> sincronizar(@Valid @RequestBody SolicitudSincronizacion solicitud) {
        // 202 Accepted: el trabajo quedó encolado; su avance se consulta con GET /sincronizaciones/{id}
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(sincronizacion.iniciar(solicitud.categoria(), solicitud.anio()));
    }

    @Operation(summary = "Historial de sincronizaciones (memoria del servidor)")
    @GetMapping("/sincronizaciones")
    public List<CargaSincronizacionDto> trabajos() {
        return sincronizacion.trabajos();
    }

    @Operation(summary = "Estado de una sincronización")
    @GetMapping("/sincronizaciones/{id}")
    public CargaSincronizacionDto estado(@PathVariable String id) {
        return sincronizacion.estado(id);
    }

    @Operation(summary = "Importar una temporada en formato JSON normalizado (ej: Turismo Carretera)")
    @PostMapping("/importaciones")
    public ResumenSincronizacion importar(@Valid @RequestBody TemporadaNorm temporada) {
        return sincronizacion.importar(temporada);
    }


}

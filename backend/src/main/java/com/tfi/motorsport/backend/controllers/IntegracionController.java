package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado;
import com.tfi.motorsport.backend.dtos.integracionAPI.ResumenSincronizacion;
import com.tfi.motorsport.backend.services.ISincronizacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
@Tag(name = "Integracion (ADMIN)")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class IntegracionController {

    private final ISincronizacionService sincronizacion;

    @Operation(summary = "Importar una temporada en formato JSON normalizado (ej: Turismo Carretera)")
    @PostMapping("/importaciones")
    public ResumenSincronizacion importar(@Valid @RequestBody ModeloNormalizado.TemporadaNorm temporada) {
        return sincronizacion.importar(temporada);
    }

}

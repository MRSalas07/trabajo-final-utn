package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.dtos.comun.PaginaDto;
import com.tfi.motorsport.backend.dtos.piloto.PerfilPilotoDto;
import com.tfi.motorsport.backend.dtos.piloto.PilotoResumenDto;
import com.tfi.motorsport.backend.services.IPilotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Listado y ficha de pilotos.
 * Los nombres de los parámetros son los que envía el frontend (interfaz Filtros de piloto.service.ts).
 */
@Tag(name = "Pilotos")
@RestController
@RequiredArgsConstructor
public class PilotoController {

    private final IPilotoService pilotoService;

    @Operation(summary = "Listado paginado de pilotos, con búsqueda por texto y filtros de categoría y año")
    @GetMapping("/api/pilotos")
    public PaginaDto<PilotoResumenDto> pilotos(
            @RequestParam(required = false) @Size(max = 60) String filPiloto,
            @RequestParam(required = false) String filtroCategoria,
            @RequestParam(required = false) @Min(1950) @Max(2100) Integer filtroAnio,
            @RequestParam(defaultValue = "0") @Min(0) int paginado,
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) int cantidad) {
        return pilotoService.buscarPilotos(filPiloto, filtroCategoria, filtroAnio, paginado, cantidad);
    }

    @Operation(summary = "Ficha de un piloto: datos, palmarés, trayectoria y últimos resultados")
    @GetMapping("/api/pilotos/{id}")
    public PerfilPilotoDto piloto(@PathVariable Long id) {
        return pilotoService.perfil(id);
    }
}

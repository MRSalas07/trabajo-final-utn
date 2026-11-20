package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.*;
import com.tfi.motorsport.backend.services.IEstadisticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Estadísticas")
@RestController
@RequestMapping("/api/estadisticas/{siglas}")
@RequiredArgsConstructor
public class EstadisticaController {

    private final IEstadisticaService  estadisticaService;

    @Operation(summary = "Resumen de la temporada: líder, ganadores por fecha, fechas disputadas")
    @GetMapping("/{anio}/resumen")
    public ResumenTemporada resumen(
            @PathVariable String siglas,
            @PathVariable @Min(1950) @Max(2100) int anio) {
        return estadisticaService.resumen(siglas, anio);
    }

    @Operation(summary = "Campeonato de pilotos calculado")
    @GetMapping("/{anio}/campeonato/pilotos")
    public List<FilaCampeonatoPiloto> pilotos(@PathVariable String siglas, @PathVariable @Min(1950) @Max(2100) int anio) {
        return estadisticaService.campeonatoPilotos(siglas, anio);
    }

    @Operation(summary = "Campeonato de equipos calculado (suma de puntos de sus pilotos)")
    @GetMapping("/{anio}/campeonato/equipos")
    public List<FilaCampeonatoEquipo> equipos(@PathVariable String siglas, @PathVariable @Min(1950) @Max(2100) int anio) {
        return estadisticaService.campeonatoEquipos(siglas, anio);
    }
}

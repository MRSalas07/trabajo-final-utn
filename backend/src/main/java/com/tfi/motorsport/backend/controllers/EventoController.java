package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.dtos.evento.EventoDto;
import com.tfi.motorsport.backend.dtos.temporada.TemporadaDto;
import com.tfi.motorsport.backend.services.IEventoService;
import com.tfi.motorsport.backend.services.ZonaHoraria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Temporadas, eventos y tablas de resultados (resultados e históricos). */
@Tag(name = "Eventos y resultados")
@RestController
@RequiredArgsConstructor
public class EventoController {
    private final IEventoService eventoService;
    private final ZonaHorariaSolicitada zonaHoraria;

    @Operation(summary = "Temporadas cargadas de una categoría")
    @GetMapping("/api/categorias/{siglas}/temporadas")
    public List<TemporadaDto> temporadas(@PathVariable String siglas) {
        return eventoService.temporadasDeCategoria(siglas);
    }

    @Operation(summary = "Eventos (fechas) de una temporada con sus sesiones o etapas")
    @GetMapping("/api/categorias/{siglas}/temporadas/{anio}/eventos")
    public List<EventoDto> eventos(@PathVariable String siglas,
                                   @PathVariable @Min(1950) @Max(2100) int anio,
                                   @RequestParam(required = false) String zona) {
        return eventoService.eventosDeTemporada(siglas, anio, zonaHoraria.resolverZonaHoraria(zona));
    }

    @Operation(summary = "Detalle de un evento")
    @GetMapping("/api/eventos/{id}")
    public EventoDto evento(@PathVariable Long id, @RequestParam(required = false) String zona) {
        return eventoService.detalle(id, zonaHoraria.resolverZonaHoraria(zona));
    }
}

package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.dtos.equipo.EquipoResumenDto;
import com.tfi.motorsport.backend.dtos.equipo.PerfilEquipoDto;
import com.tfi.motorsport.backend.services.IEquipoService;
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

/** Listado y ficha de equipos. */
@Tag(name = "Equipos")
@RestController
@RequiredArgsConstructor
public class EquipoController {

    private final IEquipoService equipoService;

    @Operation(summary = "Equipos de una categoría; el año es opcional")
    @GetMapping("/api/equipos")
    public List<EquipoResumenDto> equipos(@RequestParam String categoria,
                                          @RequestParam(required = false) @Min(1950) @Max(2100) Integer anio) {
        return equipoService.equiposDeCategoria(categoria, anio);
    }

    @Operation(summary = "Ficha de un equipo: datos, plantel, estadísticas y últimos resultados")
    @GetMapping("/api/equipos/{id}")
    public PerfilEquipoDto equipo(@PathVariable Long id) {
        return equipoService.perfil(id);
    }
}

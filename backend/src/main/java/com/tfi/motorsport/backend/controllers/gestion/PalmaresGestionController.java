package com.tfi.motorsport.backend.controllers.gestion;

import com.tfi.motorsport.backend.dtos.gestionDatos.PalmaresDtos;
import com.tfi.motorsport.backend.services.gestion.IPalmaresGestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Gestión de datos (ADMIN)")
@RestController
@RequestMapping("/api/admin/palmares")
@RequiredArgsConstructor
public class PalmaresGestionController {

    private final IPalmaresGestionService palmares;

    @Operation(summary = "Palmarés de una categoría (dato derivado; se actualiza con POST /palmares/{siglas}/recalcular)")
    @GetMapping
    public List<PalmaresDtos.Registro> listar(@RequestParam Long categoriaId) {
        return palmares.listar(categoriaId);
    }
}

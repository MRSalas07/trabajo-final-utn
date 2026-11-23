package com.tfi.motorsport.backend.controllers.gestion;

import com.tfi.motorsport.backend.dtos.gestionDatos.CategoriaDtos;
import com.tfi.motorsport.backend.services.gestion.ICategoriaGestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Listado de categorías para el panel de administración (con auditoría). El alta, la modificación
 * y la baja siguen en /api/categorias (CategoriaController), que exige ADMIN para escribir.
 */
@Tag(name = "Gestión de datos (ADMIN)")
@RestController
@RequestMapping("/api/admin/categorias")
@RequiredArgsConstructor
public class CategoriaGestionController {

    private final ICategoriaGestionService categorias;

    @Operation(summary = "Categorías no dadas de baja (activas e inactivas), con su auditoría")
    @GetMapping
    public List<CategoriaDtos.Registro> listar() {
        return categorias.listar();
    }
}

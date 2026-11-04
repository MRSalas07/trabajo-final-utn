package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.dtos.categoria.CategoriaCreateDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaUpdateDto;
import com.tfi.motorsport.backend.services.ICategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CategoriaController {

    private final ICategoriaService categoriaService;

    @Operation(summary = "Obtener todas las categorías, opcionalmente filtradas por estado")
    @GetMapping
    public ResponseEntity<List<CategoriaDto>> obtenerCategorias(
            @RequestParam(required = false) Boolean activo) {
        List<CategoriaDto> categorias = activo == null
                ? categoriaService.obtenerCategorias()
                : categoriaService.obtenerCategoriasPorFiltro(activo);
        return ResponseEntity.ok(categorias);
    }

    @Operation(summary = "Obtener categoría por ID")
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaDto> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaService.obtenerCategoria(id));
    }

    @Operation(summary = "Crear una nueva categoría")
    @PostMapping
    public ResponseEntity<CategoriaDto> crearCategoria(
            @Valid @RequestBody CategoriaCreateDto dto) {
        CategoriaDto categoria = categoriaService.crearCategoria(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoria);
    }

    @Operation(summary = "Actualizar una categoría")
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaDto> actualizarCategoria(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaUpdateDto dto) {
        return ResponseEntity.ok(categoriaService.modificarCategoria(id, dto));
    }
}

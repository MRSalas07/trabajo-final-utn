package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.dtos.categoria.CategoriaCreateDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaUpdateDto;
import com.tfi.motorsport.backend.dtos.categoria.PerfilCategoriaDto;
import com.tfi.motorsport.backend.services.ICategoriaService;
import com.tfi.motorsport.backend.services.ZonaHoraria;
import com.tfi.motorsport.backend.services.gestion.ICategoriaGestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Consulta de categorías (público) y su administración (solo ADMIN, ver SecurityConfig).
 * CORS ya no se declara aquí con "*": se configura de forma global para el origen del frontend.
 */
@Tag(name = "Categorías")
@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final ICategoriaService categoriaService;
    private final ICategoriaGestionService categoriaGestionService;

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

    @Operation(summary = "Crear una nueva categoría (ADMIN, queda registrado quién la creó)")
    @PostMapping
    public ResponseEntity<CategoriaDto> crearCategoria(
            @Valid @RequestBody CategoriaCreateDto dto, @AuthenticationPrincipal Jwt jwt) {
        CategoriaDto categoria = categoriaGestionService.crear(dto, UsuarioAutenticado.id(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(categoria);
    }

    @Operation(summary = "Actualizar una categoría (ADMIN, queda registrado quién la modificó)")
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaDto> actualizarCategoria(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaUpdateDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(categoriaGestionService.modificar(id, dto, UsuarioAutenticado.id(jwt)));
    }

    @Operation(summary = "Perfil de una categoría: temporadas, líder, último ganador y próximo evento")
    @GetMapping("/{siglas}/perfil")
    public PerfilCategoriaDto perfil(@PathVariable String siglas, @RequestParam(required = false) String zona) {
        return categoriaService.obtenerPerfil(siglas, ZonaHoraria.resolverZonaHoraria(zona));
    }

    @Operation(summary = "Dar de baja una categoría (baja lógica, ADMIN)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCategoria(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        categoriaGestionService.eliminar(id, UsuarioAutenticado.id(jwt));
        return ResponseEntity.noContent().build();
    }

}

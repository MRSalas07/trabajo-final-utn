package com.tfi.motorsport.backend.controllers.gestion;

import com.tfi.motorsport.backend.controllers.UsuarioAutenticado;
import com.tfi.motorsport.backend.dtos.gestionDatos.TemporadaDtos;
import com.tfi.motorsport.backend.services.gestion.ITemporadaGestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gestión de temporadas (solo ADMIN: todo /api/admin/** lo exige SecurityConfig).
 * Misma convención en todos los controladores de gestión:
 *   GET ?filtro (listado) · POST (201) · PUT /{id} · DELETE /{id} (baja lógica, 204)
 */
@Tag(name = "Gestión de datos (ADMIN)")
@RestController
@RequestMapping("/api/admin/temporadas")
@RequiredArgsConstructor
public class TemporadaGestionController {

    private final ITemporadaGestionService temporadas;

    @Operation(summary = "Temporadas de una categoría")
    @GetMapping
    public List<TemporadaDtos.Registro> listar(@RequestParam Long categoriaId) {
        return temporadas.listar(categoriaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemporadaDtos.Registro crear(@Valid @RequestBody TemporadaDtos.Request r, @AuthenticationPrincipal Jwt jwt) {
        return temporadas.crear(r, UsuarioAutenticado.id(jwt));
    }

    @PutMapping("/{id}")
    public TemporadaDtos.Registro modificar(@PathVariable Long id, @Valid @RequestBody TemporadaDtos.Request r,
                                            @AuthenticationPrincipal Jwt jwt) {
        return temporadas.modificar(id, r, UsuarioAutenticado.id(jwt));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        temporadas.eliminar(id, UsuarioAutenticado.id(jwt));
    }
}

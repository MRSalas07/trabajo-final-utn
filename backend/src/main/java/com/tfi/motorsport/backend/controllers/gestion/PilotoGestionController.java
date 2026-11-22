package com.tfi.motorsport.backend.controllers.gestion;

import com.tfi.motorsport.backend.controllers.UsuarioAutenticado;
import com.tfi.motorsport.backend.dtos.gestionDatos.PilotoDtos;
import com.tfi.motorsport.backend.services.gestion.IPilotoGestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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

@Tag(name = "Gestión de datos (ADMIN)")
@RestController
@RequestMapping("/api/admin/pilotos")
@RequiredArgsConstructor
public class PilotoGestionController {

    private final IPilotoGestionService pilotos;

    @Operation(summary = "Buscar pilotos para edición (hasta 500)")
    @GetMapping
    public List<PilotoDtos.Registro> listar(@RequestParam(required = false) @Size(max = 60) String q,
                                            @RequestParam(defaultValue = "500") @Min(1) @Max(500) int limit) {
        return pilotos.listar(q, limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PilotoDtos.Registro crear(@Valid @RequestBody PilotoDtos.Request r, @AuthenticationPrincipal Jwt jwt) {
        return pilotos.crear(r, UsuarioAutenticado.id(jwt));
    }

    @PutMapping("/{id}")
    public PilotoDtos.Registro modificar(@PathVariable Long id, @Valid @RequestBody PilotoDtos.Request r,
                                         @AuthenticationPrincipal Jwt jwt) {
        return pilotos.modificar(id, r, UsuarioAutenticado.id(jwt));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        pilotos.eliminar(id, UsuarioAutenticado.id(jwt));
    }
}

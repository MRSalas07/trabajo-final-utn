package com.tfi.motorsport.backend.controllers.gestion;

import com.tfi.motorsport.backend.controllers.UsuarioAutenticado;
import com.tfi.motorsport.backend.dtos.gestionDatos.ParticipacionDtos;
import com.tfi.motorsport.backend.services.gestion.IParticipacionGestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/api/admin/participaciones")
@RequiredArgsConstructor
public class ParticipacionGestionController {

    private final IParticipacionGestionService participaciones;

    @Operation(summary = "Participaciones (piloto en equipo) de una categoría y año")
    @GetMapping
    public List<ParticipacionDtos.Registro> listar(@RequestParam Long categoriaId,
                                                   @RequestParam(required = false) @Min(1950) @Max(2100) Integer anio) {
        return participaciones.listar(categoriaId, anio);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipacionDtos.Registro crear(@Valid @RequestBody ParticipacionDtos.Request r, @AuthenticationPrincipal Jwt jwt) {
        return participaciones.crear(r, UsuarioAutenticado.id(jwt));
    }

    @PutMapping("/{id}")
    public ParticipacionDtos.Registro modificar(@PathVariable Long id, @Valid @RequestBody ParticipacionDtos.Request r,
                                                @AuthenticationPrincipal Jwt jwt) {
        return participaciones.modificar(id, r, UsuarioAutenticado.id(jwt));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        participaciones.eliminar(id, UsuarioAutenticado.id(jwt));
    }
}

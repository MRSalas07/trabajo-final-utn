package com.tfi.motorsport.backend.controllers.gestion;

import com.tfi.motorsport.backend.controllers.UsuarioAutenticado;
import com.tfi.motorsport.backend.dtos.gestionDatos.TripulacionDtos;
import com.tfi.motorsport.backend.services.gestion.ITripulacionGestionService;
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
@RequestMapping("/api/admin/tripulaciones")
@RequiredArgsConstructor
public class TripulacionGestionController {

    private final ITripulacionGestionService tripulaciones;

    @Operation(summary = "Tripulaciones (piloto + copiloto) de una categoría y año")
    @GetMapping
    public List<TripulacionDtos.Registro> listar(@RequestParam Long categoriaId,
                                                 @RequestParam(required = false) @Min(1950) @Max(2100) Integer anio) {
        return tripulaciones.listar(categoriaId, anio);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripulacionDtos.Registro crear(@Valid @RequestBody TripulacionDtos.Request r, @AuthenticationPrincipal Jwt jwt) {
        return tripulaciones.crear(r, UsuarioAutenticado.id(jwt));
    }

    @PutMapping("/{id}")
    public TripulacionDtos.Registro modificar(@PathVariable Long id, @Valid @RequestBody TripulacionDtos.Request r,
                                              @AuthenticationPrincipal Jwt jwt) {
        return tripulaciones.modificar(id, r, UsuarioAutenticado.id(jwt));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        tripulaciones.eliminar(id, UsuarioAutenticado.id(jwt));
    }
}

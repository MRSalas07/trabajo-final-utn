package com.tfi.motorsport.backend.controllers.gestion;

import com.tfi.motorsport.backend.controllers.UsuarioAutenticado;
import com.tfi.motorsport.backend.dtos.gestionDatos.EtapaDtos;
import com.tfi.motorsport.backend.services.gestion.IEtapaGestionService;
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

@Tag(name = "Gestión de datos (ADMIN)")
@RestController
@RequestMapping("/api/admin/etapas")
@RequiredArgsConstructor
public class EtapaGestionController {

    private final IEtapaGestionService etapas;

    @Operation(summary = "Etapas (tramos) de un evento de rally")
    @GetMapping
    public List<EtapaDtos.Registro> listar(@RequestParam Long eventoId) {
        return etapas.listar(eventoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EtapaDtos.Registro crear(@Valid @RequestBody EtapaDtos.Request r, @AuthenticationPrincipal Jwt jwt) {
        return etapas.crear(r, UsuarioAutenticado.id(jwt));
    }

    @PutMapping("/{id}")
    public EtapaDtos.Registro modificar(@PathVariable Long id, @Valid @RequestBody EtapaDtos.Request r,
                                        @AuthenticationPrincipal Jwt jwt) {
        return etapas.modificar(id, r, UsuarioAutenticado.id(jwt));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        etapas.eliminar(id, UsuarioAutenticado.id(jwt));
    }
}

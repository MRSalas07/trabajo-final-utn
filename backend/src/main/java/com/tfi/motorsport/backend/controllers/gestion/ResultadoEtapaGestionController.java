package com.tfi.motorsport.backend.controllers.gestion;

import com.tfi.motorsport.backend.controllers.UsuarioAutenticado;
import com.tfi.motorsport.backend.dtos.gestionDatos.ResultadoEtapaDtos;
import com.tfi.motorsport.backend.services.gestion.IResultadoEtapaGestionService;
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
@RequestMapping("/api/admin/resultados-etapa")
@RequiredArgsConstructor
public class ResultadoEtapaGestionController {

    private final IResultadoEtapaGestionService resultados;

    @Operation(summary = "Resultados (tiempos) de una etapa de rally")
    @GetMapping
    public List<ResultadoEtapaDtos.Registro> listar(@RequestParam Long etapaId) {
        return resultados.listar(etapaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResultadoEtapaDtos.Registro crear(@Valid @RequestBody ResultadoEtapaDtos.Request r, @AuthenticationPrincipal Jwt jwt) {
        return resultados.crear(r, UsuarioAutenticado.id(jwt));
    }

    @PutMapping("/{id}")
    public ResultadoEtapaDtos.Registro modificar(@PathVariable Long id, @Valid @RequestBody ResultadoEtapaDtos.Request r,
                                                 @AuthenticationPrincipal Jwt jwt) {
        return resultados.modificar(id, r, UsuarioAutenticado.id(jwt));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        resultados.eliminar(id, UsuarioAutenticado.id(jwt));
    }
}

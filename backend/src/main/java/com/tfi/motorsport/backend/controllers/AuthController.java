package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.dtos.auth.AuthDto;
import com.tfi.motorsport.backend.dtos.auth.RegisterDto;
import com.tfi.motorsport.backend.services.IAuthService;
import com.tfi.motorsport.backend.services.ZonaHoraria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.tfi.motorsport.backend.dtos.auth.AuthDto.*;
import com.tfi.motorsport.backend.dtos.auth.RegisterDto;

/** Registro, login y preferencias del usuario. */
@Tag(name = "Usuarios")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @Operation(summary = "Registrar un usuario nuevo (devuelve el token JWT)")
    @PostMapping("/api/auth/registro")
    public ResponseEntity<TokenResponse> registro(@Valid @RequestBody RegisterDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @Operation(summary = "Iniciar sesión (devuelve el token JWT)")
    @PostMapping("/api/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
    @Operation(summary = "Datos del usuario autenticado")
    @GetMapping("/api/usuarios/me")
    public UsuarioDto userMe(@AuthenticationPrincipal Jwt jwt) {

        return authService.perfil(ZonaSolicitadaController.usuarioActual(jwt));
    }

    @Operation(summary = "Guardar la zona horaria preferida (devuelve un token actualizado)")
    @PutMapping("/api/usuarios/me/preferencias")
    public TokenResponse preferencias(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PreferenciasRequest request) {
        return authService.actualizarPreferencias(ZonaSolicitadaController.usuarioActual(jwt), request);
    }

}

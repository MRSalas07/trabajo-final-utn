package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.dtos.auth.AuthDto.*;
import com.tfi.motorsport.backend.dtos.auth.RegisterDto;

import com.tfi.motorsport.backend.services.IAuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Pruebas unitarias del controlador de autenticación.
 * Se llama directamente a sus métodos con el servicio simulado (mock): se verifica que
 * delegue en el servicio con los datos correctos y que responda con el código HTTP esperado.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private IAuthService service;

    @InjectMocks
    private AuthController controller;

    private static final UsuarioDto USUARIO =
            new UsuarioDto(7L, "Ana", "Pérez", "ana@mail.com", "UTC", List.of("USER"));
    private static final TokenResponse TOKEN =
            new TokenResponse("token-jwt", Instant.parse("2026-01-01T00:00:00Z"), USUARIO);

    /** Token de un usuario logueado con id 7 (claim "sub"). */
    private static Jwt jwtDelUsuario7() {
        return Jwt.withTokenValue("token").header("alg", "HS256").subject("7").build();
    }

    @Test
    void registro_devuelve201ConElToken() {
        // Arrange
        RegisterDto request = new RegisterDto("Ana", "Pérez", "ana@mail.com", "clave1234", "UTC");
        when(service.registrar(request)).thenReturn(TOKEN);

        // Act
        ResponseEntity<TokenResponse> respuesta = controller.registro(request);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(TOKEN);
    }

    @Test
    void login_devuelveElTokenDelServicio() {
        // Arrange
        LoginRequest request = new LoginRequest("ana@mail.com", "clave1234");
        when(service.login(request)).thenReturn(TOKEN);

        // Act
        TokenResponse respuesta = controller.login(request);

        // Assert
        assertThat(respuesta.token()).isEqualTo("token-jwt");
        assertThat(respuesta.usuario().email()).isEqualTo("ana@mail.com");
    }

    @Test
    void yo_buscaElPerfilDelUsuarioDelToken() {
        // Arrange
        when(service.perfil(7L)).thenReturn(USUARIO);

        // Act
        UsuarioDto respuesta = controller.obtenerUserAuth(jwtDelUsuario7());

        // Assert
        assertThat(respuesta).isEqualTo(USUARIO);
        verify(service).perfil(7L);
    }

    @Test
    void preferencias_actualizaLaZonaDelUsuarioDelToken() {
        // Arrange
        PreferenciasRequest request = new PreferenciasRequest("America/Argentina/Buenos_Aires");
        when(service.actualizarPreferencias(7L, request)).thenReturn(TOKEN);

        // Act
        TokenResponse respuesta = controller.preferencias(jwtDelUsuario7(), request);

        // Assert
        assertThat(respuesta).isEqualTo(TOKEN);
        verify(service).actualizarPreferencias(7L, request);
    }
}
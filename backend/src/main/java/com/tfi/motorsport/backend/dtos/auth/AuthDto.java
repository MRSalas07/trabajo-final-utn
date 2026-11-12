package com.tfi.motorsport.backend.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * DTOs del módulo de usuarios. Se agrupan como records anidados porque son pequeños
 * y siempre se usan juntos (registro, login, respuesta con token y perfil).
 */
public final class AuthDto {

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record TokenResponse(
            String token,
            Instant expira,
            UsuarioDto usuario
    ) {
    }

    public record UsuarioDto(
            Long id,
            String nombre,
            String apellido,
            String email,
            String zonaHoraria,
            List<String> roles
    ) {
    }

    public record PreferenciasRequest(
            @NotBlank(message = "La zona horaria es obligatoria") @Size(max = 64) String zonaHoraria
    ) {
    }
}

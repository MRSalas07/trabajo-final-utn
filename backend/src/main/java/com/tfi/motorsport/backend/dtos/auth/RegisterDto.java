package com.tfi.motorsport.backend.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDto (

            @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String nombre,
            @Size(max = 80) String apellido,
            @NotBlank(message = "El email es obligatorio") @Email(message = "El email no es válido") String email,
            @NotBlank @Size(min = 8, message = "La contraseña debe tener minimo 8 caracteres")
            String password,
            /** Zona IANA, ej: "America/Argentina/Buenos_Aires". Si no se envía se usa UTC. */
            @Size(max = 64) String zonaHoraria
) {
}

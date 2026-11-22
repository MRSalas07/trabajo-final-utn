package com.tfi.motorsport.backend.dtos.gestionDatos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

import static com.tfi.motorsport.backend.dtos.gestionDatos.ValidacionesAbm.URL;

/**
 * DTOs del ABM de pilotos.
 */
public final class PilotoDtos {

    private PilotoDtos() {
    }

    public record Request(
            @Size(max = 80) String nombre,
            @NotBlank(message = "El apellido es obligatorio") @Size(max = 80) String apellido,
            @Past(message = "La fecha de nacimiento debe ser pasada") LocalDate fechaNacimiento,
            @Size(max = 60) String nacionalidad,
            @Size(max = 60) String pais,
            @Pattern(regexp = "^[A-Za-z]{0,10}$", message = "El código debe tener hasta 10 letras") String codigo,
            @Min(0) @Max(999) Integer numero,
            @Size(max = 500) @Pattern(regexp = URL, message = "La foto debe ser una URL http(s)") String fotoUrl,
            LocalDate fechaInicioCarrera,
            LocalDate fechaFinCarrera
    ) {
    }

    public record Registro(Long id, String idExterno, String nombre, String apellido, LocalDate fechaNacimiento,
                           String nacionalidad, String pais, String codigo, Integer numero, String fotoUrl,
                           LocalDate fechaInicioCarrera, LocalDate fechaFinCarrera) {
    }
}

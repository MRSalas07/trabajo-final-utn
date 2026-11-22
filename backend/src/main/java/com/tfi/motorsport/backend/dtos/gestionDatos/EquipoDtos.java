package com.tfi.motorsport.backend.dtos.gestionDatos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

import static com.tfi.motorsport.backend.dtos.gestionDatos.ValidacionesAbm.URL;

/**
 * DTOs del ABM de equipos.
 */
public final class EquipoDtos {

    private EquipoDtos() {
    }

    public record Request(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
            /** Marca o nombre corto (ej: "Toyota", "Ford"): se usa en las estadísticas por marca. */
            @Size(max = 60) String nombreCorto,
            @Size(max = 60) String nacionalidad,
            @Size(max = 500) @Pattern(regexp = URL, message = "El logo debe ser una URL http(s)") String logoUrl,
            @Past LocalDate fechaFundacion,
            @Size(max = 120) String sede
    ) {
    }

    public record Registro(Long id, String idExterno, String nombre, String nombreCorto, String nacionalidad,
                           String logoUrl, LocalDate fechaFundacion, String sede) {
    }
}
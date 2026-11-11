package com.tfi.motorsport.backend.dtos.categoria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoriaUpdateDto(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombre,
        @NotBlank(message = "Las siglas son obligatorias")
        @Pattern(regexp = "[A-Za-z0-9]{1,10}", message = "Las siglas deben tener entre 1 y 10 letras o números")
        String siglas,
        @Size(max = 500) String descripcion,
        String paisOrigen,
        String logoUrl,
        Boolean activo
) {
}

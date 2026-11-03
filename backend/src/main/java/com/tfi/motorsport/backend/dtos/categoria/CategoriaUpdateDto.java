package com.tfi.motorsport.backend.dtos.categoria;

import jakarta.validation.constraints.NotBlank;

public record CategoriaUpdateDto(
        @NotBlank String nombre,
        @NotBlank String siglas,
        String descripcion,
        String paisOrigen,
        String logoUrl
) {
}

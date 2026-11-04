package com.tfi.motorsport.backend.dtos.categoria;

import com.tfi.motorsport.backend.model.Categoria;

public record CategoriaDto (
    Long id,
    String nombre,
    String siglas,
    String descripcion,
    String paisOrigen,
    String logoUrl,
    Boolean activo
) {
    public static CategoriaDto fromEntity(Categoria categoria) {
        return new CategoriaDto(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getSiglas(),
                categoria.getDescripcion(),
                categoria.getPaisOrigen(),
                categoria.getLogoUrl(),
                categoria.getActivo()
        );
    }
}

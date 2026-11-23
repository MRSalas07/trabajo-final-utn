package com.tfi.motorsport.backend.dtos.gestionDatos;

/**
 * Listado de categorías del panel de administración: los mismos datos que CategoriaDto (público)
 * más la auditoría, que no se publica fuera del panel porque incluye emails de usuarios.
 * El alta y la modificación siguen usando CategoriaCreateDto y CategoriaUpdateDto.
 */
public final class CategoriaDtos {

    private CategoriaDtos() {
    }

    public record Registro(Long id, String nombre, String siglas, String descripcion, String paisOrigen,
                           String logoUrl, Boolean activo, AuditoriaDto auditoria) {
    }
}

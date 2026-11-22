package com.tfi.motorsport.backend.services.gestion;

/**
 * Baja lógica de categorías. El alta y la modificación están en ICategoriaService
 * (vienen del MVP 1, antes de que existiera la gestión de datos).
 */
public interface ICategoriaGestionService {

    void eliminar(Long id, Long usuarioId);
}

package com.tfi.motorsport.backend.services.gestion;

import com.tfi.motorsport.backend.dtos.categoria.CategoriaCreateDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaUpdateDto;
import com.tfi.motorsport.backend.dtos.gestionDatos.CategoriaDtos;

import java.util.List;

/**
 * Gestión de categorías desde el panel de administración: listado con auditoría, alta y modificación
 * registrando quién las hizo, y baja lógica. Las reglas del alta y la modificación (siglas únicas,
 * normalización) siguen en ICategoriaService, que viene del MVP 1: acá se reutilizan.
 */
public interface ICategoriaGestionService {

    List<CategoriaDtos.Registro> listar();

    CategoriaDto crear(CategoriaCreateDto dto, Long usuarioId);

    CategoriaDto modificar(Long id, CategoriaUpdateDto dto, Long usuarioId);

    void eliminar(Long id, Long usuarioId);
}

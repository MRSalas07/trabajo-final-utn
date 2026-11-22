package com.tfi.motorsport.backend.services.gestion;

import com.tfi.motorsport.backend.dtos.gestionDatos.TemporadaDtos;

import java.util.List;

/**
 * Gestión de temporadas. El usuarioId es el administrador autenticado (claim "sub" del JWT) y queda en la auditoría.
 * La baja es lógica y oculta también los eventos de la temporada.
 */
public interface ITemporadaGestionService {

    List<TemporadaDtos.Registro> listar(Long categoriaId);

    TemporadaDtos.Registro crear(TemporadaDtos.Request r, Long usuarioId);

    TemporadaDtos.Registro modificar(Long id, TemporadaDtos.Request r, Long usuarioId);

    void eliminar(Long id, Long usuarioId);
}

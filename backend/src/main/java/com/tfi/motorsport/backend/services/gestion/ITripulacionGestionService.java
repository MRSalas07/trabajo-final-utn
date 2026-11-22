package com.tfi.motorsport.backend.services.gestion;

import com.tfi.motorsport.backend.dtos.gestionDatos.TripulacionDtos;

import java.util.List;

/**
 * Gestión de tripulaciones de rally (piloto + copiloto + equipo en una temporada).
 * Al crear una tripulación se registran automáticamente las participaciones del piloto y del copiloto.
 */
public interface ITripulacionGestionService {

    List<TripulacionDtos.Registro> listar(Long categoriaId, Integer anio);

    TripulacionDtos.Registro crear(TripulacionDtos.Request r, Long usuarioId);

    TripulacionDtos.Registro modificar(Long id, TripulacionDtos.Request r, Long usuarioId);

    void eliminar(Long id, Long usuarioId);
}

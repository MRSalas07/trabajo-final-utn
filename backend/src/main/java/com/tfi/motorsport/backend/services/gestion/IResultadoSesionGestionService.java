package com.tfi.motorsport.backend.services.gestion;

import com.tfi.motorsport.backend.dtos.gestionDatos.ResultadoSesionDtos;

import java.util.List;

/**
 * Gestión de resultados de sesión. En una sesión no puede haber dos pilotos con la misma posición,
 * y al cargar un resultado se registra la participación del piloto en el equipo.
 */
public interface IResultadoSesionGestionService {

    List<ResultadoSesionDtos.Registro> listar(Long sesionId);

    ResultadoSesionDtos.Registro crear(ResultadoSesionDtos.Request r, Long usuarioId);

    ResultadoSesionDtos.Registro modificar(Long id, ResultadoSesionDtos.Request r, Long usuarioId);

    void eliminar(Long id, Long usuarioId);
}

package com.tfi.motorsport.backend.services.gestion;

import com.tfi.motorsport.backend.dtos.gestionDatos.ResultadoEtapaDtos;

import java.util.List;

/** Gestión de resultados de etapa. La tripulación debe ser de la categoría y temporada del rally. */
public interface IResultadoEtapaGestionService {

    List<ResultadoEtapaDtos.Registro> listar(Long etapaId);

    ResultadoEtapaDtos.Registro crear(ResultadoEtapaDtos.Request r, Long usuarioId);

    ResultadoEtapaDtos.Registro modificar(Long id, ResultadoEtapaDtos.Request r, Long usuarioId);

    void eliminar(Long id, Long usuarioId);
}

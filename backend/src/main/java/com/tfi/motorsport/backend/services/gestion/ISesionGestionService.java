package com.tfi.motorsport.backend.services.gestion;

import com.tfi.motorsport.backend.dtos.gestionDatos.SesionDtos;

import java.util.List;

/** Gestión de sesiones (práctica, clasificación, sprint, carrera...). */
public interface ISesionGestionService {

    List<SesionDtos.Registro> listar(Long eventoId);

    SesionDtos.Registro crear(SesionDtos.Request r, Long usuarioId);

    SesionDtos.Registro modificar(Long id, SesionDtos.Request r, Long usuarioId);

    void eliminar(Long id, Long usuarioId);
}

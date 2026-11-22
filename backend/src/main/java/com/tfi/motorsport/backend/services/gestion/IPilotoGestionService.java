package com.tfi.motorsport.backend.services.gestion;

import com.tfi.motorsport.backend.dtos.gestionDatos.PilotoDtos;

import java.util.List;

/** Gestión de pilotos. No se puede dar de baja un piloto con resultados o tripulaciones activas (409). */
public interface IPilotoGestionService {

    /** Búsqueda por texto, con un máximo de "limite" registros (tope SoporteGestion.LIMITE_LISTADO). */
    List<PilotoDtos.Registro> listar(String texto, int limite);

    PilotoDtos.Registro crear(PilotoDtos.Request r, Long usuarioId);

    PilotoDtos.Registro modificar(Long id, PilotoDtos.Request r, Long usuarioId);

    void eliminar(Long id, Long usuarioId);
}

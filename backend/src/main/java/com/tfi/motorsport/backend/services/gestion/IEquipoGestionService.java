package com.tfi.motorsport.backend.services.gestion;

import com.tfi.motorsport.backend.dtos.gestionDatos.EquipoDtos;

import java.util.List;

/** Gestión de equipos. No se puede dar de baja un equipo con resultados o tripulaciones activas (409). */
public interface IEquipoGestionService {

    /** Búsqueda por texto, con un máximo de "limite" registros (tope SoporteGestion.LIMITE_LISTADO). */
    List<EquipoDtos.Registro> listar(String texto, int limite);

    EquipoDtos.Registro crear(EquipoDtos.Request r, Long usuarioId);

    EquipoDtos.Registro modificar(Long id, EquipoDtos.Request r, Long usuarioId);

    void eliminar(Long id, Long usuarioId);
}

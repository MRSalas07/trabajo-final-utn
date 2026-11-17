package com.tfi.motorsport.backend.services;

import com.tfi.motorsport.backend.dtos.equipo.EquipoResumenDto;
import com.tfi.motorsport.backend.dtos.equipo.PerfilEquipoDto;

import java.util.List;

/** Listado y ficha de equipos. */
public interface IEquipoService {

    /** Equipos con participaciones en la categoría; el año es opcional. */
    List<EquipoResumenDto> equiposDeCategoria(String siglas, Integer anio);

    PerfilEquipoDto perfil(Long equipoId);
}

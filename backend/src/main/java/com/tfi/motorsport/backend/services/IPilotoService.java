package com.tfi.motorsport.backend.services;

import com.tfi.motorsport.backend.dtos.comun.PaginaDto;
import com.tfi.motorsport.backend.dtos.piloto.PerfilPilotoDto;
import com.tfi.motorsport.backend.dtos.piloto.PilotoResumenDto;

/** Listado y ficha de pilotos. */
public interface IPilotoService {

    /**
     * @param texto   búsqueda por nombre, apellido o código (opcional)
     * @param siglas  categoría (opcional)
     * @param anio    temporada (opcional)
     * @param pagina  número de página, desde 0
     * @param tamanio cantidad de pilotos por página
     */
    PaginaDto<PilotoResumenDto> buscarPilotos(String texto, String siglas, Integer anio, int pagina, int tamanio);

    PerfilPilotoDto perfil(Long pilotoId);
}

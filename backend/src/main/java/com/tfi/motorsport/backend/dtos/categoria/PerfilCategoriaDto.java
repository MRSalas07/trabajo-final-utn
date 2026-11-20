package com.tfi.motorsport.backend.dtos.categoria;

import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.*;
import com.tfi.motorsport.backend.dtos.evento.EventoDto;
import com.tfi.motorsport.backend.dtos.temporada.TemporadaDto;

import java.util.List;

/** Perfil de una categoría: su ficha, temporadas y un resumen de la temporada actual. */
public record PerfilCategoriaDto(
        CategoriaDto categoria,
        List<TemporadaDto> temporadas,
        TemporadaDto temporadaActual,
        ResumenTemporada resumen,
        EventoDto proximoEvento,
        EventoDto ultimoEvento) {
}

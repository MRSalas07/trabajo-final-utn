package com.tfi.motorsport.backend.dtos.equipo;

import com.tfi.motorsport.backend.dtos.perfil.ParticipacionDto;
import com.tfi.motorsport.backend.dtos.perfil.ResultadoRecienteDto;

import java.time.LocalDate;
import java.util.List;

/** Ficha completa de un equipo (interfaz PerfilEquipo del frontend). */
public record PerfilEquipoDto(
        EquipoResumenDto equipo,
        String sede,
        LocalDate fechaFundacion,
        List<String> categorias,
        List<ParticipacionDto> plantel,
        EstadisticasEquipoDto estadisticas,
        List<ResultadoRecienteDto> ultimosResultados
) {
}

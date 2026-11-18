package com.tfi.motorsport.backend.dtos.piloto;

import com.tfi.motorsport.backend.dtos.perfil.ParticipacionDto;
import com.tfi.motorsport.backend.dtos.perfil.ResultadoRecienteDto;

import java.util.List;

/** Ficha completa de un piloto (interfaz PerfilPiloto del frontend). */
public record PerfilPilotoDto(
        PilotoDto piloto,
        List<PalmaresDto> palmares,
        List<ParticipacionDto> trayectoria,
        List<ResultadoRecienteDto> ultimosResultados
) {
}

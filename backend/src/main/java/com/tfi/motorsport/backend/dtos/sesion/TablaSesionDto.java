package com.tfi.motorsport.backend.dtos.sesion;

import com.tfi.motorsport.backend.dtos.evento.EventoResumenDto;
import com.tfi.motorsport.backend.dtos.resultados.ResultadoSesionDto;

import java.util.List;

public record TablaSesionDto(
        EventoResumenDto evento,
        SesionDto sesion,
        List<ResultadoSesionDto> resultados) {
}

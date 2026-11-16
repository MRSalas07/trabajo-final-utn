package com.tfi.motorsport.backend.dtos.sesion;

import com.tfi.motorsport.backend.common.Horario;
import com.tfi.motorsport.backend.dtos.evento.EventoResumenDto;
import com.tfi.motorsport.backend.dtos.resultados.ResultadoSesionDto;

import java.util.List;

public record SesionDto(Long id,
                        String nombre,
                        String tipo,
                        Integer numero,
                        Horario inicio,
                        Horario fin,
                        String estado,
                        long cantidadResultados) {
}


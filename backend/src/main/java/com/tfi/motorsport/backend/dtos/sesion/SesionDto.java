package com.tfi.motorsport.backend.dtos.sesion;

import com.tfi.motorsport.backend.common.Horario;

public record SesionDto(Long id,
                        String nombre,
                        String tipo,
                        Integer numero,
                        Horario inicio,
                        Horario fin,
                        String estado,
                        long cantidadResultados) {
}

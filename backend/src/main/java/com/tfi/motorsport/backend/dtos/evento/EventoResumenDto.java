package com.tfi.motorsport.backend.dtos.evento;

import com.tfi.motorsport.backend.dtos.categoria.CategoriaResumenDto;

public record EventoResumenDto(Long id,
                            String nombre,
                            Integer anio,
                            CategoriaResumenDto categoria) {
}

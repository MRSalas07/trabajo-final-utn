package com.tfi.motorsport.backend.dtos.temporada;

import java.time.LocalDate;

public record TemporadaDto(Long id,
                           Long categoriaId,
                           String categoria,
                           Integer anio,
                           String nombre,
                           LocalDate fechaInicio,
                           LocalDate fechaFin) {
}

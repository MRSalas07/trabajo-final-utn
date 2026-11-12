package com.tfi.motorsport.backend.dtos.etapa;

import com.tfi.motorsport.backend.common.Horario;

import java.math.BigDecimal;

public record EtapaDto(Long id,
                       Integer numero,
                       String nombre,
                       Integer dia,
                       BigDecimal distancia,
                       Horario inicio,
                       String estado,
                       long cantidadResultados,
                       boolean clasificacionGeneral)
{
}

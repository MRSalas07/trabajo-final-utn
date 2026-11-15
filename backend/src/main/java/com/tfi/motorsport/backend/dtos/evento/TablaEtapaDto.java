package com.tfi.motorsport.backend.dtos.evento;

import com.tfi.motorsport.backend.dtos.etapa.*;
import com.tfi.motorsport.backend.dtos.resultados.ResultadoEtapaDto;

import java.util.List;

public record TablaEtapaDto(
        EventoResumenDto evento,
        EtapaDto etapa,
        List<ResultadoEtapaDto> resultados
) {
}

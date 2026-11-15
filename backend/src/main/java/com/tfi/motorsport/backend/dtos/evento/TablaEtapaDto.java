package com.tfi.motorsport.backend.dtos.evento;

import com.tfi.motorsport.backend.dtos.etapa.*;

import java.util.List;

public record TablaEtapaDto(
        EventoResumenDto evento,
        EtapaDto etapa) {
}

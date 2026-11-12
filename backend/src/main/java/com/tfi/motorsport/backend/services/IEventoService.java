package com.tfi.motorsport.backend.services;


import com.tfi.motorsport.backend.dtos.evento.EventoDto;
import com.tfi.motorsport.backend.dtos.evento.TablaEtapaDto;
import com.tfi.motorsport.backend.dtos.temporada.TemporadaDto;

import java.time.ZoneId;
import java.util.List;

/** Consultas de temporadas, eventos y tablas de resultados (módulo core deportivo). */
public interface IEventoService {

    List<TemporadaDto> temporadasDeCategoria(String siglas);

    List<EventoDto> eventosDeTemporada(String siglas, int anio, ZoneId zona);

    EventoDto detalle(Long eventoId, ZoneId zona);

    TablaEtapaDto resultadosDeEtapa(Long etapaId, ZoneId zona);

}

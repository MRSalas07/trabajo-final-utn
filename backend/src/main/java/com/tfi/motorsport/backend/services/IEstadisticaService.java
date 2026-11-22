package com.tfi.motorsport.backend.services;

import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.*;

import java.util.List;

public interface IEstadisticaService {

    /** Recalcula la tabla palmares (histórico por piloto) de una categoría. Devuelve filas actualizadas. */
    int recalcularPalmares(String siglas);

    List<FilaCampeonatoPiloto> campeonatoPilotos(String siglas, int anio);
    List<FilaCampeonatoEquipo> campeonatoEquipos(String siglas, int anio);
    ResumenTemporada resumen(String siglas, int anio);

    EvolucionCampeonato evolucionPilotos(String siglas, int anio, List<Long> pilotoIds, int top);

    TendenciaPiloto tendenciaPiloto(String siglas, int anio, Long pilotoId);

    ComparativaPilotos compararPilotos(String siglas, int anio, Long pilotoA, Long pilotoB);

    List<FilaMarca> rankingMarcas(String siglas, int anio);

    TendenciaGrupo tendenciaEquipo(String siglas, int anio, Long equipoId);

    TendenciaGrupo tendenciaMarca(String siglas, int anio, String marca);

    /** Puntos acumulados por fecha de los principales equipos o marcas. tipo = EQUIPO | MARCA. */
    EvolucionGrupos evolucionGrupos(String siglas, int anio, String tipo, int top);

    Historico historicoPiloto(String siglas, Long pilotoId);

    Historico historicoEquipo(String siglas, Long equipoId);

    ComparativaHistorica comparativaHistorica(String siglas, Long pilotoA, Long pilotoB);
}

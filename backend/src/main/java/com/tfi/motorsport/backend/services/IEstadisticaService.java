package com.tfi.motorsport.backend.services;

import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.*;

import java.util.List;

public interface IEstadisticaService {

    /** Recalcula la tabla palmares (histórico por piloto) de una categoría. Devuelve filas actualizadas. */
    int recalcularPalmares(String siglas);

    List<FilaCampeonatoPiloto> campeonatoPilotos(String siglas, int anio);
    List<FilaCampeonatoEquipo> campeonatoEquipos(String siglas, int anio);
    ResumenTemporada resumen(String siglas, int anio);

    EvolucionCampeonato evolucion(String siglas, int anio, List<Long> pilotoIds, int top);

    TendenciaPiloto tendencia(String siglas, int anio, Long pilotoId);

    ComparativaPilotos comparar(String siglas, int anio, Long pilotoA, Long pilotoB);

}

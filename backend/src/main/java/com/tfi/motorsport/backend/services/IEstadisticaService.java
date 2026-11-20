package com.tfi.motorsport.backend.services;

import java.util.List;

public interface IEstadisticaService {

    /** Recalcula la tabla palmares (histórico por piloto) de una categoría. Devuelve filas actualizadas. */
    int recalcularPalmares(String siglas);
}

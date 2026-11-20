package com.tfi.motorsport.backend.services;

import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.*;
import com.tfi.motorsport.backend.dtos.integracionAPI.CargaSincronizacionDto;
import com.tfi.motorsport.backend.dtos.integracionAPI.ResumenSincronizacion;

import java.util.List;

public interface ISincronizacionService {
    /** Encola la sincronización de una temporada desde la API externa de la categoría. */
    CargaSincronizacionDto iniciar(String siglas, int anio);

    CargaSincronizacionDto estado(String trabajoId);

    List<CargaSincronizacionDto> trabajos();

    ResumenSincronizacion importar(TemporadaNorm temporada);

    List<FuenteDisponible> fuentes();

    record FuenteDisponible(String categoria, String proveedor, boolean habilitada) {
    }

}

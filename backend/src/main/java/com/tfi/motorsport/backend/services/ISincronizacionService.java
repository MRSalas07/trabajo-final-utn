package com.tfi.motorsport.backend.services;

import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.*;
import com.tfi.motorsport.backend.dtos.integracionAPI.ResumenSincronizacion;

public interface ISincronizacionService {
    ResumenSincronizacion importar(TemporadaNorm temporada);
}

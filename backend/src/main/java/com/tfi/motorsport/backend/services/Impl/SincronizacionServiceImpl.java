package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.*;
import com.tfi.motorsport.backend.clients.normalization.NormalizadorService;
import com.tfi.motorsport.backend.dtos.integracionAPI.ResumenSincronizacion;
import com.tfi.motorsport.backend.services.ISincronizacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SincronizacionServiceImpl implements ISincronizacionService {
    private final NormalizadorService normalizador;

    @Override
    public ResumenSincronizacion importar(TemporadaNorm temporada) {
        String fuente = temporada.fuente() != null ? temporada.fuente() : "IMPORTACION_JSON";
        TemporadaNorm conFuente = new TemporadaNorm(temporada.siglasCategoria().toUpperCase(Locale.ROOT),
                temporada.anio(), temporada.nombre(), fuente, temporada.eventos());
        return normalizador.persistir(conFuente, List.of());
    }
}



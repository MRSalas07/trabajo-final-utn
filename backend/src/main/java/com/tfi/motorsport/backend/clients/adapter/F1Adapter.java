package com.tfi.motorsport.backend.clients.adapter;

import com.tfi.motorsport.backend.clients.clientHttpExternal.ClientHttpExternal;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.*;
import com.tfi.motorsport.backend.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Slf4j
@Component
public class F1Adapter implements FuenteDatosCategoria{

    static final String PROVEEDOR = "JOLPICA";
    private static final int TAMANIO_PAGINA = 100;

    private final ClientHttpExternal cliente;

    public F1Adapter(AppProperties props, JsonMapper jsonMapper) {
        AppProperties.Integracion cfg = props.integracion();
        this.cliente = new ClientHttpExternal(cfg.f1().urlBase(), jsonMapper,PROVEEDOR,
                cfg.timeoutConexionMs(), cfg.timeoutLecturaMs(), cfg.reintentos(), cfg.f1().intervaloMinimoMs());
    }

    @Override
    public String siglasCategoria() {
        return "";
    }

    @Override
    public String proveedor() {
        return "";
    }

    @Override
    public TemporadaNorm obtenerTemporada(int anio, List<String> avisos) {
        return null;
    }
}

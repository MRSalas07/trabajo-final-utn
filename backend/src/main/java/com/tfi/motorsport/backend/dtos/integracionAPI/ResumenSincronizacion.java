package com.tfi.motorsport.backend.dtos.integracionAPI;

import java.time.Instant;
import java.util.List;

public record ResumenSincronizacion (
    String categoria,
    Integer anio,
    String fuente,
    int eventosProcesados,
    int eventosOmitidos,
    int sesionesProcesadas,
    int etapasProcesadas,
    int resultadosProcesados,
    int pilotosNuevos,
    int equiposNuevos,
    List<String> avisos,
    Instant inicio,
    Instant fin,
    long duracionMs
){

}

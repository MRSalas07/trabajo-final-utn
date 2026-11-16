package com.tfi.motorsport.backend.dtos.evento;

import com.tfi.motorsport.backend.common.Horario;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaResumenDto;
import com.tfi.motorsport.backend.dtos.etapa.EtapaDto;
import com.tfi.motorsport.backend.dtos.sesion.SesionDto;

import java.util.List;

public record EventoDto(
        Long id,
        CategoriaResumenDto categoria,
        Long temporadaId,
        Integer anio,
        Integer numeroFecha,
        String nombre,
        String nombreCorto,
        String pais,
        String ciudad,
        String circuito,
        /** Zona horaria de la sede (IANA). */
        String zonaHorariaSede,
        Horario inicio,
        Horario fin,
        /** Hora de inicio en la sede (ej: hora local de Melbourne). */
        String inicioHoraSede,
        /** Diferencia horaria sede - usuario en minutos (positiva = la sede va adelantada). */
        Integer diferenciaSedeMinutos,
        /** La misma diferencia en texto, ej: "La sede está 14 h adelantada respecto de tu hora". */
        String diferenciaSedeTexto,
        String estado,
        List<SesionDto> sesiones,
        List<EtapaDto> etapas
) {
}


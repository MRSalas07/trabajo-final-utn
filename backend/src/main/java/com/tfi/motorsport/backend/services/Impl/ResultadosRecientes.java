package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.dtos.perfil.ResultadoRecienteDto;
import com.tfi.motorsport.backend.model.Catalogo;
import com.tfi.motorsport.backend.repositories.unificacion.FilaResultado;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Arma la lista de "últimos resultados" que muestran las fichas de piloto y de equipo.
 *
 * El resultado de un evento es la carrera principal (F1 y TC) o la clasificación general
 * del rally (WRC, etapa "GENERAL"). Clasificaciones, sprints, series y victorias de tramo
 * no cuentan como resultado del evento.
 */
public final class ResultadosRecientes {

    /** Cantidad de resultados que se muestran en una ficha. */
    public static final int LIMITE = 10;

    private static final Comparator<FilaResultado> MAS_RECIENTE_PRIMERO = Comparator
            .comparing(FilaResultado::eventoInicio, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(FilaResultado::posicion, Comparator.nullsLast(Comparator.naturalOrder()));

    private ResultadosRecientes() {
    }

    /** true si la fila es el resultado final de un evento (carrera o clasificación general). */
    public static boolean esResultadoDeEvento(FilaResultado fila) {
        return Catalogo.TipoSesion.CARRERA.equals(fila.tipo()) || Catalogo.ETAPA_GENERAL.equals(fila.tipo());
    }

    /**
     * @param filas        resultados de sesiones y de etapas, en cualquier orden
     * @param unoPorEvento true en la ficha del piloto (un resultado por evento);
     *                     false en la del equipo (pueden aparecer sus dos pilotos en el mismo evento)
     */
    public static List<ResultadoRecienteDto> de(List<FilaResultado> filas, boolean unoPorEvento) {
        Stream<FilaResultado> recientes = filas.stream()
                .filter(ResultadosRecientes::esResultadoDeEvento)
                .sorted(MAS_RECIENTE_PRIMERO);
        if (unoPorEvento) {
            Set<Long> eventosVistos = new HashSet<>();
            recientes = recientes.filter(f -> eventosVistos.add(f.eventoId()));
        }
        return recientes.limit(LIMITE).map(ResultadosRecientes::aDto).toList();
    }

    private static ResultadoRecienteDto aDto(FilaResultado f) {
        return new ResultadoRecienteDto(f.eventoId(), f.eventoNombre(), f.categoria(), f.anio(), f.eventoInicio(),
                f.posicion(), f.estado(), f.puntos(), f.equipoNombre(), f.nombreCompleto());
    }
}

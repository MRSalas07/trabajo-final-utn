package com.tfi.motorsport.backend.repositories.unificacion;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Proyección "plana" de un resultado, común a las tres categorías.
 *
 * Es la materia prima del módulo estadístico: unifica en una sola forma
 * los resultados de sesiones (F1 y TC) y de etapas (WRC), para que los
 * cálculos (campeonato, evolución, tendencias) no dependan de la categoría.
 *
 * tipo: tipo de sesión (CARRERA, SPRINT, CLASIFICACION, SERIE...) o, para WRC,
 *       "GENERAL" (clasificación final del rally) / "ETAPA" (tramo cronometrado).
 */
public record FilaResultado(
        Long eventoId,
        Integer numeroFecha,
        String eventoNombre,
        Instant eventoInicio,
        Long temporadaId,
        Integer anio,
        /** Siglas de la categoría (F1, WRC, TC). */
        String categoria,
        Long pilotoId,
        String pilotoNombre,
        String pilotoApellido,
        String pilotoCodigo,
        Long equipoId,
        String equipoNombre,
        /** Marca / fabricante (equipo.nombre_corto: "Toyota", "Ford", "McLaren"...). */
        String equipoMarca,
        String tipo,
        Integer posicion,
        BigDecimal puntos,
        String estado
) {
    /** Copia de la fila con otra marca (la de la participación del piloto, ver MarcasPorParticipacion). */
    public FilaResultado conMarca(String marca) {
        return new FilaResultado(eventoId, numeroFecha, eventoNombre, eventoInicio, temporadaId, anio, categoria,
                pilotoId, pilotoNombre, pilotoApellido, pilotoCodigo, equipoId, equipoNombre, marca,
                tipo, posicion, puntos, estado);
    }

    public String nombreCompleto() {
        return ((pilotoNombre != null ? pilotoNombre + " " : "") + (pilotoApellido != null ? pilotoApellido : "")).trim();
    }
}


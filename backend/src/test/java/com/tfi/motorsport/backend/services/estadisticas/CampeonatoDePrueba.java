package com.tfi.motorsport.backend.services.estadisticas;

import com.tfi.motorsport.backend.repositories.unificacion.FilaResultado;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Datos de prueba compartidos por los tests de estadísticas.
 *
 * Temporada 2025 (3 fechas):
 *   Alonso    (Rojo / Ford)   25 + 18 + 15        = 58
 *   Colapinto (Rojo / Ford)   15 + 15 + 25        = 55
 *   Hamilton  (Azul / Toyota) 18 + 33 (sprint) + 0 = 51 (abandona en la fecha 3)
 *
 * Temporada 2026 (2 fechas): Hamilton gana las dos (25 + 25) y Alonso es segundo (18 + 18).
 */
final class CampeonatoDePrueba {

    static final long ALONSO = 1L;
    static final long HAMILTON = 2L;
    static final long COLAPINTO = 3L;

    static final long EQUIPO_ROJO = 10L;
    static final long EQUIPO_AZUL = 20L;

    private static final String FINALIZADO = "FINALIZADO";
    private static final String ABANDONO = "ABANDONO";

    private CampeonatoDePrueba() {
    }

    static List<FilaResultado> temporada2025() {
        List<FilaResultado> filas = new ArrayList<>();
        filas.add(fila(1, 1, 2025, ALONSO, "CARRERA", 1, 25, FINALIZADO));
        filas.add(fila(1, 1, 2025, HAMILTON, "CARRERA", 2, 18, FINALIZADO));
        filas.add(fila(1, 1, 2025, COLAPINTO, "CARRERA", 3, 15, FINALIZADO));
        filas.add(fila(1, 1, 2025, HAMILTON, "CLASIFICACION", 1, 0, FINALIZADO));
        filas.add(fila(1, 1, 2025, ALONSO, "CLASIFICACION", 2, 0, FINALIZADO));

        filas.add(fila(2, 2, 2025, HAMILTON, "CARRERA", 1, 25, FINALIZADO));
        filas.add(fila(2, 2, 2025, ALONSO, "CARRERA", 2, 18, FINALIZADO));
        filas.add(fila(2, 2, 2025, COLAPINTO, "CARRERA", 3, 15, FINALIZADO));
        filas.add(fila(2, 2, 2025, HAMILTON, "SPRINT", 1, 8, FINALIZADO));

        filas.add(fila(3, 3, 2025, COLAPINTO, "CARRERA", 1, 25, FINALIZADO));
        filas.add(fila(3, 3, 2025, ALONSO, "CARRERA", 3, 15, FINALIZADO));
        filas.add(fila(3, 3, 2025, HAMILTON, "CARRERA", 3, 0, ABANDONO));
        return filas;
    }

    static List<FilaResultado> temporada2026() {
        List<FilaResultado> filas = new ArrayList<>();
        for (int numeroFecha = 1; numeroFecha <= 2; numeroFecha++) {
            long eventoId = 100 + numeroFecha;
            filas.add(fila(eventoId, numeroFecha, 2026, HAMILTON, "CARRERA", 1, 25, FINALIZADO));
            filas.add(fila(eventoId, numeroFecha, 2026, ALONSO, "CARRERA", 2, 18, FINALIZADO));
        }
        return filas;
    }

    static List<FilaResultado> dosTemporadas() {
        List<FilaResultado> filas = new ArrayList<>(temporada2025());
        filas.addAll(temporada2026());
        return filas;
    }

    private static FilaResultado fila(long eventoId, int numeroFecha, int anio, long pilotoId,
                                      String tipoSesion, Integer posicion, int puntos, String estado) {
        long equipoId = pilotoId == HAMILTON ? EQUIPO_AZUL : EQUIPO_ROJO;
        return new FilaResultado(
                eventoId, numeroFecha, "GP " + anio + " fecha " + numeroFecha,
                Instant.parse(anio + "-0" + numeroFecha + "-10T15:00:00Z"),
                anio == 2025 ? 1L : 2L, anio, "F1",
                pilotoId, nombreDe(pilotoId), apellidoDe(pilotoId), apellidoDe(pilotoId).substring(0, 3).toUpperCase(),
                equipoId, equipoId == EQUIPO_ROJO ? "Rojo" : "Azul", equipoId == EQUIPO_ROJO ? "Ford" : "Toyota",
                tipoSesion, posicion, BigDecimal.valueOf(puntos), estado);
    }

    private static String nombreDe(long pilotoId) {
        if (pilotoId == ALONSO) return "Fernando";
        if (pilotoId == HAMILTON) return "Lewis";
        return "Franco";
    }

    private static String apellidoDe(long pilotoId) {
        if (pilotoId == ALONSO) return "Alonso";
        if (pilotoId == HAMILTON) return "Hamilton";
        return "Colapinto";
    }
}

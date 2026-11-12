package com.tfi.motorsport.backend.model;

import java.util.Set;

/**
 * Valores normalizados que se guardan en las columnas de texto "tipo" y "estado".
 * de los eventos
 */
public class Catalogo {

    private Catalogo() {
    }
    /** Tipos de sesión de una categoría de circuito (F1, TC). */
    public static final class TipoSesion {
        public static final String PRACTICA = "PRACTICA";
        public static final String CLASIFICACION = "CLASIFICACION";
        public static final String SPRINT_CLASIFICACION = "SPRINT_CLASIFICACION";
        public static final String SPRINT = "SPRINT";
        /** Series clasificatorias del Turismo Carretera. */
        public static final String SERIE = "SERIE";
        /** Carrera principal / final: define la posición del evento en las estadísticas. */
        public static final String CARRERA = "CARRERA";

        public static final Set<String> TODOS = Set.of(PRACTICA, CLASIFICACION, SPRINT_CLASIFICACION, SPRINT, SERIE, CARRERA);

        private TipoSesion() {
        }
    }

    /** Estados de evento, sesión y etapa. */
    public static final class EstadoEvento {
        public static final String PROGRAMADO = "PROGRAMADO";
        public static final String EN_CURSO = "EN_CURSO";
        public static final String FINALIZADO = "FINALIZADO";
        public static final String CANCELADO = "CANCELADO";

        public static final Set<String> TODOS = Set.of(PROGRAMADO, EN_CURSO, FINALIZADO, CANCELADO);

        private EstadoEvento() {
        }
    }

    /** Estado de un participante en un resultado. */
    public static final class EstadoResultado {
        public static final String FINALIZADO = "FINALIZADO";
        public static final String ABANDONO = "ABANDONO";
        public static final String DESCALIFICADO = "DESCALIFICADO";
        public static final String NO_LARGO = "NO_LARGO";

        public static final Set<String> TODOS = Set.of(FINALIZADO, ABANDONO, DESCALIFICADO, NO_LARGO);

        private EstadoResultado() {
        }
    }

    public static final String ETAPA_GENERAL = "GENERAL";

    /** Roles en equipo_x_piloto. */
    public static final String ROL_PILOTO = "PILOTO";
    public static final String ROL_COPILOTO = "COPILOTO";

}

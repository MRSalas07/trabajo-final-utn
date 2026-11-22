package com.tfi.motorsport.backend.dtos.gestionDatos;

/**
 * Expresiones regulares que comparten varios DTOs del ABM.
 * Son constantes (static final) porque las anotaciones de validación solo aceptan valores fijos.
 */
public final class ValidacionesAbm {

    private ValidacionesAbm() {
    }

    public static final String ESTADOS = "PROGRAMADO|EN_CURSO|FINALIZADO|CANCELADO";
    public static final String ESTADOS_RESULTADO = "FINALIZADO|ABANDONO|DESCALIFICADO|NO_LARGO";
    public static final String URL = "^(https?://.*)?$";
}

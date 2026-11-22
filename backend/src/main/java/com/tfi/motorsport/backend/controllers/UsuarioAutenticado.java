package com.tfi.motorsport.backend.controllers;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Id del usuario autenticado para la auditoría de gestión (claim "sub" del JWT).
 */
public final class UsuarioAutenticado {

    private UsuarioAutenticado() {
    }

    /** Devuelve null si el token no trae un id numérico (ej: en pruebas): la operación se hace sin auditoría. */
    public static Long id(Jwt jwt) {
        try {
            return ZonaHorariaSolicitada.usuarioActual(jwt);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.services.ZonaHoraria;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/**
 * Decide en qué zona horaria responder para el tema de los horarios de eventos:
 *  1) parámetro ?zona= (el frontend envía la del navegador o la elegida por el usuario);
 *  2) si no viene y el usuario inició sesión, su preferencia guardada (claim "zona" del JWT);
 *  3) UTC.
 */
@Component
public class ZonaHorariaSolicitada {
    public ZoneId resolverZonaHoraria(String zonaParametro) {
        if (zonaParametro != null && !zonaParametro.isBlank()) {
            return ZonaHoraria.resolverZonaHoraria(zonaParametro);
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String zona = jwt.getClaimAsString("zona");
            if (zona != null && ZonaHoraria.esValida(zona)) {
                return ZoneId.of(zona);
            }
        }
        return ZoneId.of("UTC");
    }

    /** Id del usuario autenticado (claim "sub" del JWT). */
    public static Long usuarioActual(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}

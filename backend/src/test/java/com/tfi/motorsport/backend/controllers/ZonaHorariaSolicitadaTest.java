package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas del Controller de ZonaHoraria : decide en qué zona horaria responder.
 */
class ZonaHorariaSolicitadaTest {

    private final ZonaHorariaSolicitada zonaHoraria = new ZonaHorariaSolicitada();

    /** Simula un usuario logueado cuyo token trae el claim "zona". */
    private static void loguearConZona(String zona) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject("7").claim("zona", zona).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolver_conParametro_usaElParametro() {
        // Arrange
        loguearConZona("Europe/Madrid");   // el parámetro tiene prioridad sobre la preferencia

        // Act
        ZoneId zona = zonaHoraria.resolverZonaHoraria("America/Argentina/Buenos_Aires");

        // Assert
        assertThat(zona).isEqualTo(ZoneId.of("America/Argentina/Buenos_Aires"));
    }

    @Test
    void resolver_sinParametroYLogueado_usaLaPreferenciaDelToken() {
        // Arrange
        loguearConZona("Europe/Madrid");

        // Act
        ZoneId zona = zonaHoraria.resolverZonaHoraria(null);

        // Assert
        assertThat(zona).isEqualTo(ZoneId.of("Europe/Madrid"));
    }

    @Test
    void resolver_sinParametroNiSesion_usaUTC() {
        // Arrange: nadie logueado (contexto vacío)

        // Act
        ZoneId zona = zonaHoraria.resolverZonaHoraria("  ");

        // Assert
        assertThat(zona).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    void resolver_conZonaInvalidaEnElToken_usaUTC() {
        // Arrange
        loguearConZona("Marte/Olimpo");

        // Act
        ZoneId zona = zonaHoraria.resolverZonaHoraria(null);

        // Assert
        assertThat(zona).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    void resolver_conParametroInvalido_lanzaError() {
        // Act + Assert
        assertThatThrownBy(() -> zonaHoraria.resolverZonaHoraria("Marte/Olimpo"))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessageContaining("Zona horaria inválida");
    }

    @Test
    void usuarioActual_devuelveElIdDelClaimSub() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject("42").build();

        // Act
        Long id = zonaHoraria.usuarioActual(jwt);

        // Assert
        assertThat(id).isEqualTo(42L);
    }

}
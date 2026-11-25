package com.tfi.motorsport.backend.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias de la obtención del id del usuario para la auditoría.
 * Cada prueba sigue el patrón AAA: Arrange (preparar), Act (ejecutar), Assert (verificar).
 */
class UsuarioAutenticadoTest {

    private static Jwt tokenConSubject(String subject) {
        return Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
    }

    @Test
    void id_conSubjectNumerico_devuelveElIdDelUsuario() {
        // Arrange
        Jwt token = tokenConSubject("42");

        // Act
        Long id = UsuarioAutenticado.id(token);

        // Assert
        assertThat(id).isEqualTo(42L);
    }

    @Test
    void id_conSubjectNoNumerico_devuelveNullYLaOperacionSeHaceSinAuditoria() {
        // Arrange
        Jwt token = tokenConSubject("usuario-de-prueba");

        // Act
        Long id = UsuarioAutenticado.id(token);

        // Assert
        assertThat(id).isNull();
    }
}

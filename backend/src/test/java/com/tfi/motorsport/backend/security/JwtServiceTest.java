package com.tfi.motorsport.backend.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.tfi.motorsport.backend.config.AppProperties;
import com.tfi.motorsport.backend.model.Usuario;
import com.tfi.motorsport.backend.security.JwtService.TokenGenerado;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Los tokens se generan y se validan con las clases REALES de Spring Security (las mismas que configura
 * SecurityConfig), sin simular nada: así se comprueba que un token válido se acepta y que uno vencido,
 * firmado con otra clave o modificado se rechaza.
 */
class JwtServiceTest {

    private static final String CLAVE = "clave-de-prueba-de-al-menos-32-caracteres!!";
    private static final String OTRA_CLAVE = "otra-clave-distinta-de-al-menos-32-caracteres";

    private static SecretKey clave(String texto) {
        return new SecretKeySpec(texto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** El mismo armado que SecurityConfig.jwtEncoder() y jwtDecoder(). */
    private static JwtService servicio(String claveSecreta, long expiracionMinutos) {
        AppProperties props = new AppProperties(new AppProperties.Jwt(claveSecreta, expiracionMinutos),
                new AppProperties.Admin("admin@motorsport.local"), null, null, null);
        return new JwtService(new NimbusJwtEncoder(new ImmutableSecret<>(clave(claveSecreta))), props);
    }

    private static JwtDecoder decodificador(String claveSecreta) {
        return NimbusJwtDecoder.withSecretKey(clave(claveSecreta)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private static Usuario usuario(long id, String email) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Ana");
        usuario.setEmail(email);
        usuario.setZonaHoraria("America/Argentina/Buenos_Aires");
        return usuario;
    }

    @Test
    void elTokenLlevaLosDatosDelUsuarioYVenceSegunLaConfiguracion() {
        TokenGenerado generado = servicio(CLAVE, 240).generarToken(usuario(7, "ana@ejemplo.com"));

        Jwt jwt = decodificador(CLAVE).decode(generado.token());

        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("ana@ejemplo.com");
        assertThat(jwt.getClaimAsString("nombre")).isEqualTo("Ana");
        assertThat(jwt.getClaimAsString("zona")).isEqualTo("America/Argentina/Buenos_Aires");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getExpiresAt()).isCloseTo(Instant.now().plus(Duration.ofMinutes(240)), within(Duration.ofSeconds(5)));
        // El JWT guarda la fecha en segundos enteros
        assertThat(generado.expira()).isCloseTo(jwt.getExpiresAt(), within(Duration.ofSeconds(1)));
    }

    @Test
    void unEmailDeAdministrador_recibeElRolAdmin_sinImportarMayusculas() {
        TokenGenerado generado = servicio(CLAVE, 60).generarToken(usuario(1, "ADMIN@Motorsport.local"));

        assertThat(generado.roles()).containsExactly("USER", "ADMIN");
        assertThat(decodificador(CLAVE).decode(generado.token()).getClaimAsStringList("roles"))
                .containsExactly("USER", "ADMIN");
    }

    @Test
    void unTokenVencido_seRechaza() {
        // Un token de 4 horas validado "5 horas después": se adelanta el reloj del validador de Spring
        String token = servicio(CLAVE, 240).generarToken(usuario(7, "ana@ejemplo.com")).token();
        NimbusJwtDecoder dentroDeCincoHoras = NimbusJwtDecoder.withSecretKey(clave(CLAVE)).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator validador = new JwtTimestampValidator();
        validador.setClock(Clock.offset(Clock.systemUTC(), Duration.ofHours(5)));
        dentroDeCincoHoras.setJwtValidator(validador);

        assertThatThrownBy(() -> dentroDeCincoHoras.decode(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void unTokenFirmadoConOtraClave_seRechaza() {
        String deOtroServidor = servicio(OTRA_CLAVE, 60).generarToken(usuario(7, "ana@ejemplo.com")).token();

        assertThatThrownBy(() -> decodificador(CLAVE).decode(deOtroServidor)).isInstanceOf(JwtException.class);
    }

    @Test
    void unTokenModificado_seRechaza() {
        // Alguien cambia el contenido (por ejemplo, para darse el rol ADMIN) sin poder volver a firmarlo
        String[] partes = servicio(CLAVE, 60).generarToken(usuario(7, "ana@ejemplo.com")).token().split("\\.");
        String contenidoFalso = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"7\",\"roles\":[\"USER\",\"ADMIN\"]}".getBytes(StandardCharsets.UTF_8));
        String alterado = partes[0] + "." + contenidoFalso + "." + partes[2];

        assertThatThrownBy(() -> decodificador(CLAVE).decode(alterado)).isInstanceOf(JwtException.class);
    }
}

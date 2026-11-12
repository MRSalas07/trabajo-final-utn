package com.tfi.motorsport.backend.security;

import com.tfi.motorsport.backend.config.AppProperties;
import com.tfi.motorsport.backend.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera los tokens JWT que el frontend envía en el header "Authorization: Bearer ...".
 *
 * Claims incluidos:
 *  - sub: id del usuario
 *  - email, nombre, zona: datos útiles para el frontend (no sensibles)
 *  - roles: ["USER"] o ["USER","ADMIN"] si el email está en ADMIN_EMAILS
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder encoder;
    private final AppProperties props;

    public record TokenGenerado(String token, Instant expira, List<String> roles) {
    }

    public TokenGenerado generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(props.jwt().expiracionMinutos(), ChronoUnit.MINUTES);
        List<String> roles = rolesDe(usuario);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("motorsport-analytics")
                .issuedAt(ahora)
                .expiresAt(expira)
                .subject(String.valueOf(usuario.getId()))
                .claim("email", usuario.getEmail())
                .claim("nombre", usuario.getNombre())
                .claim("zona", usuario.getZonaHoraria())
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenGenerado(token, expira, roles);
    }

    /** La tabla usuario no tiene columna de rol: el rol ADMIN se asigna por configuración. */
    public List<String> rolesDe(Usuario usuario) {
        List<String> roles = new ArrayList<>(List.of("USER"));
        if (usuario.getEmail() != null
                && props.admin().emailsNormalizados().contains(usuario.getEmail().toLowerCase())) {
            roles.add("ADMIN");
        }
        return roles;
    }
}

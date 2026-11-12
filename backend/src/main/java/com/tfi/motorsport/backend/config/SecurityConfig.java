package com.tfi.motorsport.backend.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.tfi.motorsport.backend.dtos.error.ErrorDto;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final AppProperties props;
    private final JsonMapper jsonMapper;

    public SecurityConfig(AppProperties props, JsonMapper jsonMapper) {
        this.props = props;
        this.jsonMapper = jsonMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API REST sin cookies de sesión: CSRF no aplica
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Documentación y salud
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/api/salud").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Registro y login
                        .requestMatchers("/api/auth/**").permitAll()
                        // Administración
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/categorias/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categorias/**").hasRole("ADMIN")
                        // Perfil del usuario logueado
                        .requestMatchers("/api/usuarios/**").authenticated()
                        // El resto de las consultas es público
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint((req, res, ex) ->
                                escribirError(res, HttpStatus.UNAUTHORIZED, "No autenticado",
                                        "Token ausente, inválido o expirado")))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                escribirError(res, HttpStatus.UNAUTHORIZED, "No autenticado",
                                        "Debe iniciar sesión para acceder a este recurso"))
                        .accessDeniedHandler((req, res, ex) ->
                                escribirError(res, HttpStatus.FORBIDDEN, "Acceso denegado",
                                        "No tiene permisos para realizar esta operación")));
        return http.build();
    }

    /** Lee los roles del claim "roles" del token y los convierte en ROLE_xxx. */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    /** CORS: solo se aceptan peticiones desde los orígenes configurados (URL del frontend). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(props.cors().lista());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /** BCrypt: algoritmo de hash lento y con "sal", adecuado para contraseñas. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveSecreta()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(claveSecreta())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
    /** Valida que la clave exista y tenga el largo mínimo para HS256 (256 bits). */
    private SecretKey claveSecreta() {
        String secret = props.jwt() != null ? props.jwt().secret() : null;
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET no está configurado o tiene menos de 32 caracteres. Revise el archivo .env");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** Respuesta JSON con el mismo formato que el GlobalExceptionHandler. */
    private void escribirError(HttpServletResponse res, HttpStatus estado, String error, String detalle)
            throws IOException {
        res.setStatus(estado.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(res.getOutputStream(), ErrorDto.simple(estado.value(), error, detalle));
    }
}

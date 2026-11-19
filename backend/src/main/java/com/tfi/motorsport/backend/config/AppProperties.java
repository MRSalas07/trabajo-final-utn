package com.tfi.motorsport.backend.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Propiedades propias de la aplicación (prefijo "app." en application.properties).
 * Se agrupan en records inmutables: Spring los completa al iniciar a partir de
 * las variables de entorno / .env, así ninguna clave queda escrita en el código.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Admin admin,
        Cors cors,
        Integracion integracion
) {

    /** Configuración del token JWT. */
    public record Jwt(String secret, long expiracionMinutos) {
    }

    /** Lista de emails con rol ADMIN (separados por coma). */
    public record Admin(String emails) {
        public Set<String> emailsNormalizados() {
            return separarPorComa(emails).stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
        }
    }

    /** Orígenes (URLs del frontend) autorizados a llamar a la API. */
    public record Cors(String origenesPermitidos) {
        public List<String> lista() {
            return separarPorComa(origenesPermitidos);
        }
    }

    /** Parámetros comunes a todos los clientes de APIs externas. */
    public record Integracion(
            int timeoutConexionMs,
            int timeoutLecturaMs,
            int reintentos,
            Proveedor jolpica,
            ProveedorWrc wrc,
            boolean syncAutomatica,
            String syncCron
    ) {
    }

    /** Proveedor externo genérico. intervaloMinimoMs limita la frecuencia de llamadas. */
    public record Proveedor(String urlBase, long intervaloMinimoMs) {
    }

    /** WRC se puede deshabilitar porque su API es no oficial. */
    public record ProveedorWrc(String urlBase, long intervaloMinimoMs, boolean habilitado) {
    }

    private static List<String> separarPorComa(String valor) {
        if (valor == null || valor.isBlank()) {
            return List.of();
        }
        return Arrays.stream(valor.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}

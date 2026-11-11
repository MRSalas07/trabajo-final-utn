package com.tfi.motorsport.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación interactiva de la API (Swagger UI en /swagger-ui.html).
 * Se declara el esquema "bearer" para poder probar los endpoints protegidos pegando el JWT.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI motorsportOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Motorsport Analytics API")
                        .version("0.1.0")
                        .description("API REST del TFI - UTN. Categorías F1, WRC y TC: calendario, "
                                + "resultados, perfiles, estadísticas e integración de datos."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}

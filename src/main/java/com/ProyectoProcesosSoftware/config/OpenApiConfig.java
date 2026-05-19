package com.ProyectoProcesosSoftware.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {

        Info info = new Info()
                .title("EventPass API")
                .version("1.0.0")
                .description("""
                        API REST para la gestión de eventos, entradas y asistentes.

                        **Autenticación**: la mayoría de los endpoints requieren un token JWT.
                        Haz clic en **Authorize** e introduce `Bearer <tu-token>`.
                        """)
                .contact(new Contact()
                        .name("Equipo EventPass")
                        .email("dev-team@eventpass.com")
                        .url("https://github.com/ProyectoProcesosSoftware/eventpass"));

        SecurityScheme bearerScheme = new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)   // tipo HTTP (no apiKey)
                .scheme("bearer")                 // sub-tipo: Bearer
                .bearerFormat("JWT")              // hint visual en Swagger UI
                .description("Token JWT obtenido en /api/auth/login. "
                        + "Ejemplo: `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9…`");


        SecurityRequirement globalSecurityRequirement =
                new SecurityRequirement().addList(SECURITY_SCHEME_NAME);

        return new OpenAPI()
                .info(info)
                .addSecurityItem(globalSecurityRequirement)
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, bearerScheme));
    }
}
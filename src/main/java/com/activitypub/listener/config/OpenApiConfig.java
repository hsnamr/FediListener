package com.activitypub.listener.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger configuration. §12.3, §8.1.
 * API docs: /api/v3/api-docs, Swagger UI: /api/swagger-ui.html (with context-path /api).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ActivityPub Listener API")
                        .description("Social Listening and Monitoring Service for ActivityPub/Fediverse. " +
                                "All API responses use a consistent envelope: { data?, message?, error?, code? }. " +
                                "Error format (§8.1): error (message), code (RESOURCE_NOT_FOUND, VALIDATION_ERROR, " +
                                "ILLEGAL_STATE, INVALID_ARGUMENT, UNAUTHORIZED, FORBIDDEN, INTERNAL_ERROR).")
                        .version("v1")
                        .contact(new Contact().name("API Support")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer JWT"))
                .components(new Components()
                        .addSecuritySchemes("Bearer JWT",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT in Authorization or luc-authorization header")));
    }
}

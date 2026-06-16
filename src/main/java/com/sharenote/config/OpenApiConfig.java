package com.sharenote.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";
    private final String apiBasePath;

    public OpenApiConfig(@Value("${api.version.base-path:/api/v1}") String apiBasePath) {
        this.apiBasePath = apiBasePath;
    }

    @Bean
    // Builds Swagger/OpenAPI metadata for the versioned ShareNote REST API.
    public OpenAPI shareNoteOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShareNote API")
                        .version("v1")
                        .description("JWT-secured REST API for ShareNote users, notes, study groups, notifications, and admin moderation.")
                        .contact(new Contact()
                                .name("ShareNote Backend")
                                .email("no-reply@sharenote.local")))
                .servers(List.of(new Server()
                        .url(apiBasePath)
                        .description("Current API version")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}

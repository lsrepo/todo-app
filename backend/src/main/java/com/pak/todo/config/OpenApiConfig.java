package com.pak.todo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI openAPI() {
		String schemeName = "bearer-jwt";
		return new OpenAPI()
				.components(new Components()
						.addSecuritySchemes(schemeName,
								new SecurityScheme()
										.type(SecurityScheme.Type.HTTP)
										.scheme("bearer")
										.bearerFormat("JWT")
										.description("Obtain a token from POST /api/login, then paste it here. It will be sent as Authorization: Bearer <token>.")))
				.addSecurityItem(new SecurityRequirement().addList(schemeName));
	}
}

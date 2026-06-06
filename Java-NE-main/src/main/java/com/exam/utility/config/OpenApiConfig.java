package com.exam.utility.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Utility Billing System API",
        version = "1.0",
        description = "Enterprise Utility Billing System for WASAC (Water) and REG (Electricity) – Rwanda",
        contact = @Contact(name = "WASAC/REG IT Team", email = "it@wasac-reg.rw"),
        license = @License(name = "Proprietary")
    ),
    servers = {
        @Server(url = "/api/v1", description = "Default Server"),
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Provide a JWT access token obtained from /auth/login"
)
public class OpenApiConfig {
}

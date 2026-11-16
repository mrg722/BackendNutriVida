package cl.duoc.nutrivida.auth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// Con estas anotaciones describo el contrato que Springdoc publica en Swagger UI.
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "NutriVida Auth API",
                version = "1.0",
                description = "Autenticación de usuarios, emisión de JWT y gestión de roles "
                        + "de la Clínica Nutricional NutriVida"),
        // Asocio el esquema Bearer a los endpoints documentados.
        security = @SecurityRequirement(name = "bearerAuth"))
// Este esquema es el que hace aparecer el botón Authorize en Swagger UI.
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}

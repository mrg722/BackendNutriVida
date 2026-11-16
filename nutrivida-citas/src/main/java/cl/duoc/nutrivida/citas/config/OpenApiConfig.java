package cl.duoc.nutrivida.citas.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// Describo el contrato de este microservicio para que Springdoc lo publique en Swagger UI.
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "NutriVida Citas API",
                version = "1.0",
                description = "Catálogo de servicios, agenda de citas, fichas clínicas y "
                        + "estadísticas de la Clínica Nutricional NutriVida"),
        security = @SecurityRequirement(name = "bearerAuth"))
// Este esquema hace aparecer el botón Authorize, donde pego el token emitido por nutrivida-auth.
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}

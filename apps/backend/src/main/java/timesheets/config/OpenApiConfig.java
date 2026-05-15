package timesheets.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;
//Initial configuration for OpenAPI/Swagger documentation using springdoc-openapi.
//The @OpenAPIDefinition annotation provides metadata about the API, such as title, version, and description.
//The @SecurityScheme annotation defines a security scheme named "bearerAuth" that uses HTTP


@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Momently API",
        version = "1.0",
        description = "Momentum Life internal time-tracking and productivity platform. " +
                      "All endpoints except /api/auth/** require a Bearer JWT token."
    )
)
@SecurityScheme(
    name = "bearerAuth",
    description = "JWT token — obtain from POST /api/auth/login",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
    // springdoc-openapi reads the annotations above and builds Swagger UI automatically.
    // No bean methods needed, annotation-driven configuration is sufficient.
}
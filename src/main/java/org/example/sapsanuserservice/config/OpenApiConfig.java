package org.example.sapsanuserservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "SAPSAN User Service API",version = "1.0"),
        security = @SecurityRequirement(name="sapsan_auth")
)
@SecurityScheme(
        name = "sapsan_auth",
        type = SecuritySchemeType.OAUTH2,
        description = "Авторизация через Keycloak",
        flows = @OAuthFlows(
                password = @OAuthFlow(
                        tokenUrl = "https://sapsan.local/realms/sapsan/protocol/openid-connect/token"
                )
        )
)
public class OpenApiConfig {
}

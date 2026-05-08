package org.example.sapsanuserservice.config;

import io.swagger.v3.oas.annotations.security.OAuthFlows;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig {
    @Bean
    public Keycloak keycloak(){
        return KeycloakBuilder.builder()
                .serverUrl("https://sapsan.local/")
                .realm("sapsan")
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId("sapsan-gateway")
                .clientSecret("gNs9woxM1dOyoC2yRoENt7AUWKeqt5a5")
                .build();
    }
}

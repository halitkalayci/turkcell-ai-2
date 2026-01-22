package com.ecommerce.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration for Gateway Service.
 * Enables OAuth2 authentication and JWT token validation.
 * 
 * Key Features:
 * - OAuth2 login via Keycloak (Authorization Code Flow)
 * - JWT token validation for resource server mode
 * - Actuator endpoints remain public (health checks)
 * - CSRF disabled (stateless token-based security)
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            // Disable CSRF (token-based security, no sessions)
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            
            // Configure authorization rules
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints (health checks, monitoring)
                .pathMatchers("/actuator/**").permitAll()
                
                // All other requests require authentication
                .anyExchange().authenticated()
            )
            
            // Enable OAuth2 Login (redirects to Keycloak login page)
            .oauth2Login(oauth2 -> {
                // Default configuration uses registered client from application.yml
            })
            
            // Enable JWT Resource Server mode (validates Bearer tokens)
            .oauth2ResourceServer(oauth2 -> oauth2.jwt());
        
        return http.build();
    }
}

package com.ecommerce.inventoryservice.infrastructure.config;

import com.ecommerce.inventoryservice.infrastructure.security.JwtClaimsConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Inventory Service.
 * Configures JWT token validation and operation-based authorization.
 * 
 * Key Features:
 * - JWT signature validation against Keycloak
 * - Operation-based claims extraction (inventory_claims)
 * - Stateless session management (no server-side sessions)
 * - Method-level security via @PreAuthorize annotations
 * - Public access to actuator endpoints (health checks)
 * 
 * IMPORTANT: This is a Resource Server (validates JWTs from Gateway).
 * Does NOT handle OAuth2 login flow (Gateway handles that).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtClaimsConverter jwtClaimsConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (stateless token-based security)
            .csrf(csrf -> csrf.disable())
            
            // Stateless session (no server-side sessions)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (health checks, monitoring)
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll() // Dev only
                .requestMatchers("/swagger-ui/**", "/api/v1/api-docs/**").permitAll() // OpenAPI docs
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Enable JWT Resource Server
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    // Custom converter to extract inventory_claims from JWT
                    .jwtAuthenticationConverter(jwtClaimsConverter)
                )
            );
        
        // Allow H2 Console to be embedded in iframe (dev only)
        http.headers(headers -> headers
            .frameOptions(frame -> frame.sameOrigin())
        );
        
        return http.build();
    }
}

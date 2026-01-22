package com.ecommerce.orderservice.infrastructure.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extracts operation-based claims from JWT tokens for Order Service.
 * 
 * Keycloak JWT Structure:
 * {
 *   "sub": "user-id",
 *   "preferred_username": "customer1",
 *   "order_claims": ["order.create", "order.read.own", "order.cancel"]
 * }
 * 
 * This converter:
 * 1. Extracts the "order_claims" array from JWT
 * 2. Converts each claim to Spring Security GrantedAuthority
 * 3. Enables @PreAuthorize("hasAuthority('order.create')") annotations
 * 
 * NOTE: Claims are client-specific (order_claims for this service).
 * Inventory Service uses "inventory_claims" instead.
 */
@Component
public class JwtClaimsConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String CLAIMS_KEY = "order_claims";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
    }

    /**
     * Extracts order_claims from JWT and converts to GrantedAuthority collection.
     * 
     * @param jwt The JWT token from Keycloak
     * @return Collection of GrantedAuthority objects
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // Extract order_claims from JWT (may be null if user has no order permissions)
        Object claimsObject = jwt.getClaims().get(CLAIMS_KEY);
        
        if (claimsObject == null) {
            return Collections.emptyList();
        }
        
        // Handle both String (single claim) and List<String> (multiple claims)
        if (claimsObject instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<String> claims = (List<String>) claimsObject;
            
            return claims.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        } else if (claimsObject instanceof String) {
            return Collections.singletonList(new SimpleGrantedAuthority((String) claimsObject));
        }
        
        return Collections.emptyList();
    }
}

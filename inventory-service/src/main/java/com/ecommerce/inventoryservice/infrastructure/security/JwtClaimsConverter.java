package com.ecommerce.inventoryservice.infrastructure.security;

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
 * Extracts operation-based claims from JWT tokens for Inventory Service.
 * 
 * Keycloak JWT Structure:
 * {
 *   "sub": "user-id",
 *   "preferred_username": "inventory_manager",
 *   "inventory_claims": ["inventory.read", "inventory.write", "reservation.create"]
 * }
 * 
 * This converter:
 * 1. Extracts the "inventory_claims" array from JWT
 * 2. Converts each claim to Spring Security GrantedAuthority
 * 3. Enables @PreAuthorize("hasAuthority('inventory.read')") annotations
 * 
 * NOTE: Claims are client-specific (inventory_claims for this service).
 * Order Service uses "order_claims" instead.
 */
@Component
public class JwtClaimsConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String CLAIMS_KEY = "inventory_claims";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
    }

    /**
     * Extracts inventory_claims from JWT and converts to GrantedAuthority collection.
     * 
     * @param jwt The JWT token from Keycloak
     * @return Collection of GrantedAuthority objects
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // Extract inventory_claims from JWT (may be null if user has no inventory permissions)
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

# JWT Token Structure

**Last Updated:** 2026-01-22

---

## Overview

This document explains the structure of JWT (JSON Web Token) tokens issued by Keycloak for our e-commerce platform. Understanding token structure is essential for debugging authentication issues and implementing custom authorization logic.

---

## Token Anatomy

A JWT token consists of three Base64-encoded parts separated by dots:

```
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.SIGNATURE
│                                      │                                                             │
└─────────── HEADER ───────────────────┴────────────────── PAYLOAD ──────────────────────────────────┴── SIGNATURE
```

### Parts

1. **Header**: Algorithm and token type
2. **Payload**: Claims (user data, permissions, metadata)
3. **Signature**: Verification signature (RS256 with Keycloak's private key)

**Decoding**: Use https://jwt.io to decode and inspect tokens

---

## Example Token (Decoded)

### Customer Token

```json
{
  "exp": 1705926123,
  "iat": 1705925223,
  "jti": "abc-123-xyz-uuid",
  "iss": "http://localhost:8180/realms/ecommerce",
  "aud": "account",
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "typ": "Bearer",
  "azp": "gateway-client",
  "session_state": "session-uuid-here",
  "acr": "1",
  "allowed-origins": ["http://localhost:8080"],
  "realm_access": {
    "roles": ["Customer", "offline_access", "uma_authorization"]
  },
  "resource_access": {
    "order-service": {
      "roles": ["order.create", "order.read.own", "order.cancel"]
    },
    "inventory-service": {
      "roles": ["inventory.read", "reservation.create"]
    },
    "account": {
      "roles": ["manage-account", "view-profile"]
    }
  },
  "scope": "openid profile email",
  "sid": "session-id",
  "email_verified": true,
  "name": "John Doe",
  "preferred_username": "customer1",
  "given_name": "John",
  "family_name": "Doe",
  "email": "customer1@test.com",
  "order_claims": ["order.create", "order.read.own", "order.cancel"],
  "inventory_claims": ["inventory.read", "reservation.create"]
}
```

---

### Admin Token

```json
{
  "exp": 1705926123,
  "iat": 1705925223,
  "jti": "def-456-abc-uuid",
  "iss": "http://localhost:8180/realms/ecommerce",
  "aud": "account",
  "sub": "660e9500-f39c-52e5-b827-557766551111",
  "typ": "Bearer",
  "azp": "gateway-client",
  "realm_access": {
    "roles": ["Admin", "offline_access", "uma_authorization"]
  },
  "resource_access": {
    "order-service": {
      "roles": [
        "order.create",
        "order.read",
        "order.read.all",
        "order.update",
        "order.cancel",
        "order.delete",
        "order.status.update"
      ]
    },
    "inventory-service": {
      "roles": [
        "inventory.read",
        "inventory.write",
        "inventory.create",
        "inventory.delete",
        "reservation.create",
        "reservation.read",
        "reservation.confirm",
        "reservation.cancel"
      ]
    }
  },
  "scope": "openid profile email",
  "email_verified": true,
  "name": "Admin User",
  "preferred_username": "admin",
  "email": "admin@ecommerce.com",
  "order_claims": [
    "order.create",
    "order.read",
    "order.read.all",
    "order.update",
    "order.cancel",
    "order.delete",
    "order.status.update"
  ],
  "inventory_claims": [
    "inventory.read",
    "inventory.write",
    "inventory.create",
    "inventory.delete",
    "reservation.create",
    "reservation.read",
    "reservation.confirm",
    "reservation.cancel"
  ]
}
```

---

## Standard Claims

### Required Claims (RFC 7519)

| Claim | Type | Description | Example |
|-------|------|-------------|---------|
| `iss` | String | Issuer (Keycloak URL) | `http://localhost:8180/realms/ecommerce` |
| `sub` | String | Subject (User ID/UUID) | `550e8400-e29b-41d4-a716-446655440000` |
| `aud` | String/Array | Audience (intended recipient) | `account` or `["gateway", "order-service"]` |
| `exp` | Number | Expiration timestamp (Unix epoch) | `1705926123` (15 minutes from issue) |
| `iat` | Number | Issued at timestamp | `1705925223` |
| `jti` | String | JWT ID (unique identifier) | `abc-123-xyz-uuid` |

---

### Keycloak-Specific Claims

| Claim | Type | Description | Usage |
|-------|------|-------------|-------|
| `typ` | String | Token type | Always `"Bearer"` |
| `azp` | String | Authorized party (client ID) | `gateway-client` |
| `session_state` | String | Keycloak session ID | Session tracking |
| `acr` | String | Authentication context class | `"1"` (level of authentication) |
| `allowed-origins` | Array | Allowed CORS origins | `["http://localhost:8080"]` |
| `scope` | String | OAuth2 scopes (space-separated) | `"openid profile email"` |
| `sid` | String | Session ID (short form) | Alternative to `session_state` |

---

## User Information Claims

| Claim | Type | Description | Example |
|-------|------|-------------|---------|
| `preferred_username` | String | Username | `customer1` |
| `name` | String | Full name | `John Doe` |
| `given_name` | String | First name | `John` |
| `family_name` | String | Last name | `Doe` |
| `email` | String | Email address | `customer1@test.com` |
| `email_verified` | Boolean | Email verification status | `true` |

---

## Authorization Claims

### realm_access

**Type**: Object  
**Description**: Realm-level roles (composite roles)

```json
"realm_access": {
  "roles": ["Customer", "offline_access", "uma_authorization"]
}
```

**Usage**: Identify high-level user persona (Customer, Admin, etc.)

---

### resource_access

**Type**: Object  
**Description**: Client-specific roles (operation claims)

```json
"resource_access": {
  "order-service": {
    "roles": ["order.create", "order.read.own", "order.cancel"]
  },
  "inventory-service": {
    "roles": ["inventory.read", "reservation.create"]
  }
}
```

**Usage**: Fine-grained permissions for each service

---

### Custom Claims (via Mappers)

#### order_claims

**Type**: Array  
**Description**: Flattened list of order service permissions

```json
"order_claims": ["order.create", "order.read.own", "order.cancel"]
```

**Configuration**: Client scope mapper in Keycloak  
**Usage**: Easier extraction in Spring Security

---

#### inventory_claims

**Type**: Array  
**Description**: Flattened list of inventory service permissions

```json
"inventory_claims": ["inventory.read", "reservation.create"]
```

---

## Token Sizes

| User Type | Approximate Size | Claims Count |
|-----------|------------------|--------------|
| Customer | ~2.5 KB | ~15 claims |
| Admin | ~3.5 KB | ~25 claims |
| Service Account | ~2.0 KB | Minimal |

**Note**: Token size increases with number of roles/claims

---

## Token Lifecycle

### Lifespan (Configurable)

| Token Type | Default Lifespan | Configurable In |
|------------|------------------|-----------------|
| Access Token | 15 minutes | Realm Settings → Tokens |
| Refresh Token | 30 minutes | Realm Settings → Tokens |
| ID Token | 15 minutes | (same as access token) |
| SSO Session | 10 hours | Realm Settings → SSO Session Max |

---

### Validation Process

```
1. Client sends: Authorization: Bearer <TOKEN>
2. Service extracts token from header
3. Service validates:
   ✓ Signature (using Keycloak public key)
   ✓ Expiration (exp > current time)
   ✓ Issuer (iss == configured issuer-uri)
   ✓ Audience (aud contains service or account)
4. Service extracts claims for authorization
5. Business logic executes
```

---

### Refresh Flow

```
1. Access token expires (after 15 minutes)
2. Client sends refresh token to Keycloak
3. Keycloak validates refresh token
4. Keycloak issues new access token + new refresh token
5. Client uses new access token
```

**Endpoint**: `POST /realms/ecommerce/protocol/openid-connect/token`

**Request**:
```bash
curl -X POST 'http://localhost:8180/realms/ecommerce/protocol/openid-connect/token' \
  -d 'grant_type=refresh_token' \
  -d 'client_id=gateway-client' \
  -d 'client_secret=YOUR_SECRET' \
  -d 'refresh_token=REFRESH_TOKEN'
```

---

## Extracting Claims in Spring Boot

### Basic Extraction

```java
@RestController
public class OrderController {

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponseDto> getOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        
        // User ID (UUID)
        String userId = jwt.getSubject();
        
        // Username
        String username = jwt.getClaimAsString("preferred_username");
        
        // Email
        String email = jwt.getClaimAsString("email");
        
        // Issued at
        Instant issuedAt = jwt.getIssuedAt();
        
        // Expiration
        Instant expiration = jwt.getExpiresAt();
        
        // All claims
        Map<String, Object> allClaims = jwt.getClaims();
        
        // Business logic...
    }
}
```

---

### Extracting Custom Claims

```java
// Order claims (array)
List<String> orderClaims = jwt.getClaim("order_claims");

// Check specific claim
boolean canCreateOrder = orderClaims != null && 
    orderClaims.contains("order.create");

// Inventory claims
List<String> inventoryClaims = jwt.getClaim("inventory_claims");
```

---

### Extracting Resource Access

```java
// Get resource_access claim
Map<String, Object> resourceAccess = jwt.getClaim("resource_access");

if (resourceAccess != null) {
    // Get order-service roles
    Map<String, Object> orderService = 
        (Map<String, Object>) resourceAccess.get("order-service");
    
    if (orderService != null) {
        List<String> roles = (List<String>) orderService.get("roles");
        // Use roles...
    }
}
```

---

### Extracting Realm Roles

```java
// Get realm_access claim
Map<String, Object> realmAccess = jwt.getClaim("realm_access");

if (realmAccess != null) {
    List<String> roles = (List<String>) realmAccess.get("roles");
    
    boolean isAdmin = roles.contains("Admin");
    boolean isCustomer = roles.contains("Customer");
}
```

---

## JwtClaimsConverter Example

For cleaner extraction, use a custom converter:

```java
@Component
public class JwtClaimsConverter 
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // Extract from custom claim (easier)
        List<String> orderClaims = jwt.getClaim("order_claims");
        
        if (orderClaims == null) {
            return Collections.emptyList();
        }
        
        return orderClaims.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
```

**Usage in SecurityConfig**:
```java
.oauth2ResourceServer(oauth2 -> oauth2
    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtClaimsConverter))
)
```

---

## Security Considerations

### ✅ Best Practices

1. **Validate Signature**: Always verify JWT signature (Spring does this automatically)
2. **Check Expiration**: Never accept expired tokens
3. **Verify Issuer**: Ensure token is from trusted Keycloak instance
4. **Use HTTPS**: Always use TLS in production
5. **Short Lifespan**: Keep access token lifespan short (15 min)
6. **Token Storage**: Store tokens securely (httpOnly cookies or secure storage)

### ❌ Security Risks

1. **Token Leakage**: Don't log full tokens
2. **Long Expiration**: Don't set expiration > 1 hour
3. **Client-Side Storage**: Avoid localStorage (XSS risk)
4. **No Revocation Check**: JWT can't be revoked before expiration
5. **Large Tokens**: Too many claims → large tokens → performance issues

---

## Debugging Tips

### Issue: "Invalid signature"

**Cause**: Keycloak public key doesn't match

**Solution**:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # Use jwk-set-uri (recommended)
          jwk-set-uri: http://localhost:8180/realms/ecommerce/protocol/openid-connect/certs
```

---

### Issue: Token doesn't contain expected claims

**Debug Steps**:
1. Decode token at https://jwt.io
2. Check client scope mappers in Keycloak
3. Verify mapper is in `dedicated` scope (not `default`)
4. Test token endpoint directly

---

### Issue: "403 Forbidden" despite having role

**Debug Steps**:
1. Check `@PreAuthorize` annotation matches exact claim name
2. Verify `JwtClaimsConverter` extracts correct claim field
3. Enable debug logging:
   ```yaml
   logging:
     level:
       org.springframework.security: DEBUG
   ```
4. Check Spring Security logs for denied authorization

---

### Issue: Token expired quickly

**Solution**: Increase lifespan in Keycloak:
1. Realm Settings → Tokens
2. Access Token Lifespan → `30 Minutes` (or desired value)
3. Save

---

## Useful Tools

- **JWT.io**: Decode and inspect tokens → https://jwt.io
- **Keycloak Token Endpoint**: Get tokens manually
- **Postman**: Test API with Bearer tokens
- **Browser DevTools**: Inspect token in Application → Local Storage

---

## Related Documentation

- [Operation Claims Guide](operation-claims-guide.md)
- [Keycloak Integration Guide](keycloak-integration.md)
- [Security Testing Guide](security-testing-guide.md)

---

**Reference**: 
- RFC 7519 (JWT): https://tools.ietf.org/html/rfc7519
- Keycloak Token Docs: https://www.keycloak.org/docs/26.5.1/securing_apps/#_token-exchange

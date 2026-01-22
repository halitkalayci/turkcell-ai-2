# ADR-005: Keycloak with Operation-Based Claims Security Architecture

**Status**: ACCEPTED  
**Date**: 2026-01-22  
**Decision Makers**: Platform Team  
**Related**: ADR-003 (API Gateway)

---

## Context

Our e-commerce microservices platform requires a centralized authentication and authorization solution. Each service currently lacks security, and we need:

1. **Centralized Identity Management**: Single source of truth for users
2. **Fine-Grained Authorization**: Granular permissions beyond simple roles
3. **Single Sign-On (SSO)**: Users login once, access all services
4. **Scalability**: Easy to add new services and permissions
5. **Standards Compliance**: OAuth2, OpenID Connect, JWT

### Current State

- No authentication mechanism
- Services are publicly accessible
- No user management
- No authorization checks
- Security is a critical gap

### Business Requirements

- Different user types: Customers, Admins, Inventory Managers, Order Managers
- Customers can only access their own data
- Managers have elevated permissions within their domain
- Admins have full access
- Need audit trail of who did what

---

## Decision

We will implement **Keycloak 26.5.1** as our Identity and Access Management (IAM) solution with an **operation-based claims** authorization model.

### Technology Stack

**Identity Provider**: Keycloak 26.5.1
- Open-source, mature, production-ready
- Standards-compliant (OAuth2, OIDC, SAML)
- Rich admin UI for user/role management
- Extensive integration options

**Authorization Model**: Operation-Based Claims (NOT Role-Based)
- Fine-grained permissions (e.g., `order.create`, `inventory.read`)
- Claims represented as Keycloak client roles
- Composite roles group common claim combinations

**Protocol**: OAuth2 / OpenID Connect
- Industry standard
- Token-based (stateless)
- Refresh token support

**Token Format**: JWT (JSON Web Token)
- Self-contained (no database lookup needed)
- Cryptographically signed (RS256)
- Claims embedded in token

---

## Architecture

### High-Level Flow

```
┌─────────────┐
│   CLIENT    │ (Web/Mobile App)
└──────┬──────┘
       │ 1. Login Request
       ▼
┌─────────────────────────┐
│   KEYCLOAK SERVER       │ Realm: ecommerce
│   Port: 8180            │ - Users
│                         │ - Roles (Claims)
│   • Authentication      │ - Clients
│   • Token Generation    │ - Policies
└──────┬──────────────────┘
       │ 2. JWT Token (Bearer)
       ▼
┌─────────────────────────┐
│   GATEWAY SERVICE       │ OAuth2 Client + Resource Server
│   Port: 8080            │ - Validates JWT signature
│                         │ - Extracts claims
│   • Token Validation    │ - Forwards token (TokenRelay)
│   • Routing             │
└──────┬──────────────────┘
       │ 3. Forwarded Request (with token)
       ▼
┌─────────────────────────┐
│  BACKEND SERVICES       │ Resource Servers
│  • order-service:8081   │ - Validate JWT
│  • inventory:8082       │ - Extract claims
│                         │ - Authorize via @PreAuthorize
│   • Method Security     │
└─────────────────────────┘
```

---

### Components

#### 1. Keycloak Server

**Responsibilities**:
- User authentication (username/password, social login)
- Token issuance (access token, refresh token, ID token)
- Token validation (signature, expiration)
- User/role management
- Client registration

**Configuration**:
- Realm: `ecommerce`
- Clients: `gateway-client`, `order-service`, `inventory-service`
- Users: Managed via admin UI
- Token lifespan: Access (15 min), Refresh (30 min)

---

#### 2. Gateway Service

**Responsibilities**:
- OAuth2 Client (initiates login flow)
- OAuth2 Resource Server (validates tokens)
- Token relay to downstream services
- First line of defense (early authorization checks)

**Security Configuration**:
```yaml
spring:
  security:
    oauth2:
      client:
        provider:
          keycloak:
            issuer-uri: http://localhost:8180/realms/ecommerce
        registration:
          keycloak:
            client-id: gateway-client
            client-secret: <SECRET>
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/ecommerce
```

**Dependencies**:
- `spring-boot-starter-oauth2-client`
- `spring-boot-starter-oauth2-resource-server`
- `spring-cloud-starter-gateway`

---

#### 3. Backend Services (Order, Inventory)

**Responsibilities**:
- OAuth2 Resource Server (validate JWT)
- Extract user context from JWT
- Method-level authorization via `@PreAuthorize`
- Business logic execution

**Security Configuration**:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/ecommerce
          jwk-set-uri: http://localhost:8180/realms/ecommerce/protocol/openid-connect/certs
```

**Dependencies**:
- `spring-boot-starter-security`
- `spring-boot-starter-oauth2-resource-server`

---

### Operation-Based Claims Model

**Why NOT Role-Based?**
- Traditional roles (ROLE_ADMIN, ROLE_USER) are too coarse
- Cannot express "user can create orders but not delete them"
- Inflexible when business requirements change

**Operation-Based Claims**:
- Format: `<resource>.<operation>` (e.g., `order.create`)
- Each claim represents a specific permission
- Fine-grained control at endpoint level
- Easy to compose via composite roles

**Example Claims**:
```
order.create          → POST /orders
order.read.own        → GET /orders/{id} (own data)
order.read.all        → GET /orders/{id} (any data)
order.cancel          → POST /orders/{id}/cancel
order.status.update   → PATCH /orders/{id}/status
order.delete          → DELETE /orders/{id}

inventory.read        → GET /inventory/items/{id}
inventory.write       → PUT /inventory/items/{id}
reservation.create    → POST /reservations
reservation.confirm   → POST /reservations/{id}/confirm
```

**Composite Roles** (claim groups):
- **Customer**: `order.create`, `order.read.own`, `order.cancel`, `inventory.read`, `reservation.create`
- **Admin**: ALL claims
- **OrderManager**: `order.read.all`, `order.update`, `order.status.update`, `order.cancel`
- **InventoryManager**: `inventory.*`, `reservation.*`

---

### Token Flow

#### 1. Authentication (Password Grant)

```
POST /realms/ecommerce/protocol/openid-connect/token
  grant_type=password
  client_id=gateway-client
  client_secret=<SECRET>
  username=customer1
  password=customer123

Response:
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6...",
  "expires_in": 900,
  "token_type": "Bearer"
}
```

---

#### 2. Authorization (API Call)

```
POST /api/v1/orders
Authorization: Bearer <access_token>

Gateway:
  1. Validates JWT signature (via Keycloak JWK endpoint)
  2. Checks expiration
  3. Extracts claims: ["order.create", "order.read.own", ...]
  4. Forwards to order-service with same token

Order Service:
  1. Validates JWT signature (again, defense in depth)
  2. @PreAuthorize("hasAuthority('order.create')") checks claim
  3. Extracts user ID from token (sub claim)
  4. Executes business logic
  5. Returns response
```

---

#### 3. Token Refresh

```
POST /realms/ecommerce/protocol/openid-connect/token
  grant_type=refresh_token
  client_id=gateway-client
  client_secret=<SECRET>
  refresh_token=<refresh_token>

Response: New access_token + new refresh_token
```

---

### JWT Token Structure

```json
{
  "sub": "user-uuid",
  "preferred_username": "customer1",
  "email": "customer1@test.com",
  "realm_access": {
    "roles": ["Customer"]
  },
  "resource_access": {
    "order-service": {
      "roles": ["order.create", "order.read.own", "order.cancel"]
    },
    "inventory-service": {
      "roles": ["inventory.read", "reservation.create"]
    }
  },
  "order_claims": ["order.create", "order.read.own", "order.cancel"],
  "inventory_claims": ["inventory.read", "reservation.create"],
  "exp": 1705926123,
  "iat": 1705925223
}
```

**Key Claims**:
- `sub`: User ID (used for ownership checks)
- `preferred_username`: Username
- `resource_access`: Client-specific roles (operation claims)
- Custom claims (`order_claims`, `inventory_claims`): Flattened for easier extraction
- `exp`: Expiration timestamp

---

## Consequences

### Positive

✅ **Centralized Identity Management**
- Single source of truth for users
- Consistent authentication across all services
- Easy user lifecycle management (create, disable, delete)

✅ **Fine-Grained Authorization**
- Granular permissions per endpoint
- Flexible claim combinations
- Easy to add new permissions without code changes

✅ **Security Best Practices**
- Industry-standard protocols (OAuth2, OIDC)
- Stateless token-based security (scalable)
- Token expiration and refresh (balance security vs UX)
- Cryptographic signatures (tamper-proof)

✅ **Scalability**
- New services easily integrated (add client in Keycloak)
- New permissions easily added (add client role)
- Horizontal scaling (stateless tokens)

✅ **Auditability**
- Token contains user identity (sub, username)
- All operations traceable to user
- Integration with logging/monitoring

✅ **Developer Experience**
- Clear authorization model (`@PreAuthorize("hasAuthority('claim')")`)
- Easy testing (mock JWT tokens)
- Good Spring Security integration

✅ **Future Extensibility**
- Social login ready (Google, Facebook, GitHub)
- Multi-factor authentication (MFA) support
- Single Sign-On (SSO) across multiple applications
- API key authentication (service-to-service)

---

### Negative

❌ **Initial Setup Complexity**
- Keycloak installation and configuration
- Client setup, role mapping, scope mappers
- Learning curve for OAuth2/OIDC concepts
- Additional infrastructure component

❌ **Token Size**
- JWT tokens can be large (2-4 KB) with many claims
- Increased network overhead
- Cannot be easily revoked (must wait for expiration)

❌ **Operational Overhead**
- Keycloak must be highly available (single point of failure)
- Database for Keycloak (PostgreSQL)
- Monitoring and maintenance
- Backup and disaster recovery

❌ **Token Expiration UX**
- Tokens expire (security vs convenience trade-off)
- Must handle refresh flow in frontend
- Expired token errors if not handled properly

❌ **Debugging Complexity**
- Token validation errors can be cryptic
- Must understand JWT structure for troubleshooting
- Keycloak logs separate from application logs

---

### Mitigations

**For Complexity**:
- Comprehensive documentation (this ADR + guides)
- Keycloak Docker Compose setup (easy local dev)
- Test users pre-configured
- Example code in repos

**For Token Size**:
- Keep claim count reasonable
- Use short claim names
- Consider token compression (future)

**For Availability**:
- Run Keycloak in HA mode (clustered)
- External PostgreSQL (not embedded)
- Health checks and monitoring
- Backup automation

**For Token Expiration**:
- Reasonable lifespan (15 min access, 30 min refresh)
- Frontend libraries handle refresh automatically
- Clear error messages

---

## Alternatives Considered

### 1. Spring Security + Database

**Description**: Implement authentication using Spring Security with users stored in application database

**Pros**:
- No external dependency
- Full control over implementation
- Simpler deployment

**Cons**:
- ❌ Must implement in each service (code duplication)
- ❌ No SSO support
- ❌ No social login
- ❌ Reinventing the wheel
- ❌ Not scalable

**Decision**: Rejected - Too much custom code, no SSO

---

### 2. Auth0 / Okta (SaaS)

**Description**: Use managed IAM service

**Pros**:
- No infrastructure management
- Production-ready out of the box
- Excellent UX

**Cons**:
- ❌ Vendor lock-in
- ❌ Monthly costs (scales with users)
- ❌ Data residency concerns
- ❌ Less control over customization

**Decision**: Rejected - Unnecessary cost for educational project, vendor lock-in

---

### 3. Custom OAuth2 Server (Spring Authorization Server)

**Description**: Build our own OAuth2 server using Spring Authorization Server

**Pros**:
- Full control
- Spring ecosystem integration

**Cons**:
- ❌ Complex to build correctly
- ❌ Must implement user management UI
- ❌ No admin console out of the box
- ❌ Ongoing maintenance burden

**Decision**: Rejected - Keycloak provides this functionality already

---

### 4. API Keys (Service-to-Service Only)

**Description**: Simple API key authentication

**Pros**:
- Simple to implement
- No tokens needed

**Cons**:
- ❌ No user context
- ❌ No fine-grained permissions
- ❌ Keys must be securely distributed
- ❌ Revocation is hard

**Decision**: Rejected - Not suitable for user-facing APIs

---

## Implementation Plan

### Phase 1: Infrastructure Setup ✅
- [x] Docker Compose for Keycloak + PostgreSQL
- [x] Realm creation (`ecommerce`)
- [x] Client registration (gateway, services)
- [x] Test users (customer1, admin, managers)

### Phase 2: Claim Definition ✅
- [x] Define operation claims (order.*, inventory.*)
- [x] Create client roles in Keycloak
- [x] Define composite roles (Customer, Admin, etc.)
- [x] Configure client scope mappers

### Phase 3: Gateway Integration (Next)
- [ ] Add OAuth2 dependencies
- [ ] Configure SecurityFilterChain
- [ ] Implement JWT validation
- [ ] Add TokenRelay filter to routes
- [ ] Test token propagation

### Phase 4: Service Integration (Next)
- [ ] Add security dependencies to order-service
- [ ] Implement SecurityConfig + JwtClaimsConverter
- [ ] Add @PreAuthorize to controllers
- [ ] Extract user context in use cases
- [ ] Repeat for inventory-service

### Phase 5: Testing (Next)
- [ ] Unit tests with mock JWT
- [ ] Integration tests with Keycloak
- [ ] Postman collection for manual testing
- [ ] Test all permission combinations

### Phase 6: Documentation (In Progress)
- [x] ADR (this document)
- [x] Keycloak integration guide
- [x] Operation claims guide
- [x] JWT token structure guide
- [x] Security testing guide
- [ ] Update OpenAPI specs with security schemes

---

## Monitoring and Observability

### Metrics to Track

- Token issuance rate
- Token validation failures
- Authorization failures (403)
- Token expiration rate
- User login attempts (success/failure)

### Logging

- All authentication attempts (success/failure)
- Authorization denials with user + claim + endpoint
- Token refresh events
- User context in all business operations

### Alerts

- High rate of 403 errors (potential misconfiguration)
- High rate of token validation failures
- Keycloak health check failures

---

## Security Considerations

### Threats Mitigated

✅ **Unauthorized Access**: All endpoints require valid token  
✅ **Privilege Escalation**: Fine-grained claims prevent over-permission  
✅ **Token Tampering**: Cryptographic signature (RS256)  
✅ **Replay Attacks**: Token expiration + JTI claim  
✅ **CSRF**: Token-based (not cookie-based)  

### Remaining Risks

⚠️ **Token Leakage**: If token stolen, valid until expiration (15 min)  
⚠️ **Keycloak Compromise**: Single point of failure for security  
⚠️ **Insider Threats**: Admin can create/modify users  

**Mitigations**:
- Short token lifespan (15 min)
- HTTPS everywhere (TLS)
- Keycloak admin access tightly controlled
- Audit logs for all Keycloak operations
- Rate limiting on token endpoint

---

## Compliance

### GDPR
- User data stored in Keycloak (EU region possible)
- Right to be forgotten (delete user in Keycloak)
- Data portability (export user data)

### SOC 2
- Audit trail (Keycloak logs + application logs)
- Access control (operation-based claims)
- Encryption in transit (TLS)

---

## Related Documentation

- [Keycloak Integration Guide](../security/keycloak-integration.md)
- [Operation Claims Guide](../security/operation-claims-guide.md)
- [JWT Token Structure](../security/jwt-token-structure.md)
- [Security Testing Guide](../security/security-testing-guide.md)
- [AGENTS.md Section 7](../../AGENTS.md#7-security--authentication)

---

## References

- Keycloak Documentation: https://www.keycloak.org/docs/26.5.1/
- OAuth2 RFC: https://tools.ietf.org/html/rfc6749
- OpenID Connect: https://openid.net/specs/openid-connect-core-1_0.html
- JWT RFC: https://tools.ietf.org/html/rfc7519
- Spring Security OAuth2: https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html

---

**Approved by**: Platform Team  
**Review Date**: 2026-01-22  
**Next Review**: 2026-04-22 (quarterly)

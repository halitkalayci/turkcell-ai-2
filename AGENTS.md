## Mini E-Commerce Microservices Project 

This repository contains a mini e-commerce microservice ecosystem.

- Services: order-service, notification-service, inventory-service

- Tech: Java 21, Spring Boot 3.X, OpenAPI/Swagger

- Approach: Contract-First

If any rule below is violated, the output is WRONG.
You MUST NOT invent.

---

## 1) HOW TO WORK (MANDATORY WORKFLOW)

### 1.1 Plan-first, then code

Before generating code, you MUST:

- Confirm the relevant contract(s) exist and match the request. (OpenAPI)

- Propose a FILE BREAKDOWN (what files will be created/changed + why)

- Ask QUESTIONS for missing details instead of guessing.

- Generate codes in batches which are connected. (or in small batches of max 3 files)

### 1.2 No Inventing

You MUST NOT invent:

- endpoints, request/response fields, error models

- event names/payloads

- DB schema/columns

- Business rules

- Anything architectural 

If any detail is missing, ASK.

### 1.3 Documentation

- You MUST NOT create any .md file unless it is requested.

## 2) CONTRACT FIRST

### 2.1 OpenAPI is the source of truth

- API contracts live under: `/docs/openapi`

- Each service MUST have its own OpenAPI file:
  - `docs/openapi/order-service.yaml`
  - `docs/openapi/inventory-service.yaml`

- Swagger UI is generated from these contracts.

- Implementation MUST follow the contract; never the other way around.

### 2.2 Code Generation Policy

- We may use OpenAPI tooling ONLY if already present in the repository.

- You MUST NOT add new OpenAPI generator dependencies without explicit approval.

- If no generator is available, implement controllers/DTOs manually to match the spec.

### 2.3 Versioning

- API Changes MUST be versioned. (eg. `api/v1/orders`, `/api/v2/orders`)

- Version MUST be placed in path.

- Breaking changes require a new version; do not silently break clients.





## 3) ARCHITECTURE (PER SERVICE)

Each service MUST follow this layering:

`controller (web) -> application (use-cases) -> domain (business rules) -> infrastructure (persistence/clients/messaging)`

Each service MUST follow `Hexagonal Architecture` principles.

### 3.1 Controller (web)

- No business logic.

- Validates input, maps to application layer, return response DTOs.

- NEVER returns JPA entities.

### 3.2 Application (use-cases)

- Orchestrates use-cases, transaction, ports.

- MUST BE unit-testable.

### 3.3 Domain

- Contains business invariants and domain model.

- Avoid framework annotations in domain if possible.

### 3.4 Infrastructure

- JPA entities, repositories, external clients, messaging adapters.

- No business rules here.

## 4) CODING STANDARDS (QUALITY BAR)

### 4.1) Simplicity

- Prefer the simplest working solution.

- Avoid over-engineering, unnecessary patterns, extra layers.

### 4.2) Money & Time

- Money uses `BigDecimal`, never `double/float`

- Time uses `Instant` or `OffsetDateTime`

- Do not mix time types without reason.

### 4.3) IDs

- Use a single ID strategy across all services. (UUID)

- Never mix UUID and Long unless explicitly required by contract.

### 4.4) Optimistic Locking

- ALL entities with concurrent access MUST use `@Version`

- NEVER manually set both `id` and `version=null` for entities with @Version

- Mappers MUST separate:
  - `toEntity()`: New entities (no ID/version)
  - `updateEntity()`: Update existing (preserve ID/version)

- Repository adapters MUST check existence before save

- See: [optimistic-locking-guidelines.md](docs/architecture/optimistic-locking-guidelines.md)

## 5) GATEWAY RULES

### 5.1 Gateway Responsibilities

The API Gateway is the **single entry point** for all client requests.

Gateway MUST:
- Route requests to appropriate downstream services
- Inject correlation IDs (X-Correlation-Id) for traceability
- Log all requests/responses with timestamps
- Monitor downstream service health
- Transform connection errors to RFC 7807 Problem Details

Gateway MUST NOT:
- Contain business logic
- Modify request/response payloads (except headers)
- Make business decisions
- Store state (stateless routing only)

### 5.2 Adding New Services

When adding a new service, you MUST:
1. Follow `/api/v1` versioning (ADR-002)
2. Add gateway route configuration
3. Add health check integration
4. Update documentation

See `docs/gateway-integration-guidelines.md` for complete checklist.

### 5.3 Port Assignment

- Gateway: `8080` (client entry point)
- Services: `8081+` (incremental, never reuse)

Gateway is the ONLY service clients access directly.

## 6) EVENT-DRIVEN MESSAGING

### 6.1 Event Contract First
- Event contracts live under: `/docs/events`
- Messaging configuration: `docs/events/messaging-configuration.md`
- Inter-service events: Separate files per domain (e.g., `order-events.md`, `inventory-events.md`, `notification-events.md`)
- Event schemas MUST be defined before implementation
- Events are immutable once published
- Use JSON for event serialization

### 6.2 Spring Cloud Stream (Abstraction Layer)
- ALL services MUST use Spring Cloud Stream for messaging
- Do NOT use Kafka-specific APIs directly
- Use binder abstraction to support multiple message brokers (Kafka, RabbitMQ, etc.)
- Define channels via functional programming model (Supplier, Function, Consumer)
- Binder can be switched via configuration without code changes

### 6.3 Transactional Outbox Pattern (MANDATORY)
- ALL producers MUST use Transactional Outbox Pattern
- Outbox table MUST be in same database as business entities
- Outbox publisher uses polling with @Scheduled(fixedDelay = 10000)
- No direct message broker publishing from business logic
- Publisher workflow:
  1. Business logic writes to DB + outbox in same transaction
  2. Separate scheduled job polls outbox for unpublished events
  3. Publishes via Spring Cloud Stream
  4. Marks as published in outbox

### 6.4 Consumer Guarantees
- **Idempotency:** ALL consumers MUST be idempotent
  - Track processed event IDs in `processed_events` table
  - Check before processing (if exists, skip)
  - Use `eventId` as idempotency key
- **Retry:** Failed messages MUST be retried with exponential backoff
  - Max 3 retries with delays: 5s, 15s, 45s
  - Use Spring Cloud Stream retry configuration
- **DLQ:** After max retries, messages MUST go to Dead Letter Queue
  - Each service has its own DLQ destination
  - DLQ messages stored with error metadata for manual review

### 6.5 Event Structure (Standard Envelope)
ALL events MUST follow this structure:
```json
{
  "eventId": "uuid (required)",
  "eventType": "string (required)",
  "eventTimestamp": "ISO-8601 (required)",
  "correlationId": "uuid (required, for tracing)",
  "aggregateId": "uuid (required, business entity ID)",
  "payload": { }
}
```

### 6.6 Layering for Events
- **Domain:** Event domain models (business events)
- **Application:** Event handlers (use-case triggers), outbox service
- **Infrastructure:** 
  - Spring Cloud Stream bindings
  - Outbox repository & entity
  - Processed events repository
  - DLQ handlers

### 6.7 Messaging Configuration
- Current binder: Kafka (bootstrap servers: `localhost:29023`)
- All services connect to same message broker
- Destination naming: `{service-name}.{domain}.{event-type}`
- DLQ naming: `{service-name}.dlq`
- Binder can be switched to RabbitMQ or others via configuration only

## 7) SECURITY & AUTHENTICATION

### 7.1 Keycloak Integration (MANDATORY)

ALL services MUST use Keycloak for authentication & authorization.

- **Keycloak Version**: 26.5.1
- **Realm**: `ecommerce`
- **Protocol**: OAuth2 / OpenID Connect
- **Token Type**: JWT (Bearer)
- **Docker Setup**: See `docs/security/keycloak-integration.md`

### 7.2 Operation-Based Claims (NOT Role-Based)

We use **operation-based claims** for fine-grained permissions:

✅ **Correct**: `@PreAuthorize("hasAuthority('order.create')")`  
❌ **Wrong**: `@PreAuthorize("hasRole('ADMIN')")`

**Claim Naming Convention**: `<resource>.<operation>`
- Examples: `order.create`, `inventory.read`, `reservation.confirm`
- Special: `order.read.own` (user-specific), `order.read.all` (admin-level)

See: `docs/security/operation-claims-guide.md` for complete list.

### 7.3 Service Layer Security

#### Gateway Service
- **Role**: OAuth2 Client + Resource Server
- **Responsibilities**:
  - Validates JWT tokens (signature, expiration, issuer)
  - Forwards tokens to downstream services (TokenRelay filter)
  - Early authorization checks (optional, for performance)
- **Dependencies**: 
  - `spring-boot-starter-oauth2-client`
  - `spring-boot-starter-oauth2-resource-server`
  - `spring-cloud-starter-gateway`

#### Backend Services (Order, Inventory)
- **Role**: Resource Server
- **Responsibilities**:
  - Validate JWT signatures (defense in depth)
  - Extract claims for authorization
  - Use `@PreAuthorize` on controller methods
  - Extract user context from JWT (`sub`, `preferred_username`)
- **Dependencies**:
  - `spring-boot-starter-security`
  - `spring-boot-starter-oauth2-resource-server`

### 7.4 Token Propagation

- Gateway MUST use `TokenRelay=` filter for all routes
- Backend services MUST extract user context from JWT:
  ```java
  @AuthenticationPrincipal Jwt jwt
  String userId = jwt.getSubject();
  String username = jwt.getClaimAsString("preferred_username");
  List<String> orderClaims = jwt.getClaim("order_claims");
  ```

### 7.5 Security Configuration Rules

- ALL endpoints (except actuator, swagger) MUST be authenticated
- Use `SessionCreationPolicy.STATELESS` (no server-side sessions)
- CSRF protection disabled (token-based security)
- NEVER store credentials in code/config (use environment variables)
- Token lifespan: Access Token (15 min), Refresh Token (30 min)

**Example SecurityConfig**:
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt());
        return http.build();
    }
}
```

### 7.6 Controller Authorization

- Use `@PreAuthorize` on ALL controller methods
- Extract JWT for user context:
  ```java
  @PostMapping
  @PreAuthorize("hasAuthority('order.create')")
  public ResponseEntity<OrderResponseDto> createOrder(
          @Valid @RequestBody OrderRequestDto request,
          @AuthenticationPrincipal Jwt jwt) {
      
      String userId = jwt.getSubject();
      // Business logic...
  }
  ```

- For `*.own` claims, implement ownership checks in service layer:
  ```java
  public OrderResponseDto getOrder(UUID orderId, String userId, boolean canReadAll) {
      Order order = orderRepository.findById(orderId).orElseThrow();
      
      // Ownership check
      if (!canReadAll && !order.getCustomerId().equals(userId)) {
          throw new ForbiddenException("Cannot access other user's orders");
      }
      
      return mapper.toDto(order);
  }
  ```

### 7.7 JWT Claims Extraction

Backend services MUST implement `JwtClaimsConverter` to extract claims:

```java
@Component
public class JwtClaimsConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // Extract custom claims (e.g., "order_claims")
        List<String> claims = jwt.getClaim("order_claims");
        
        Collection<GrantedAuthority> authorities = claims.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        
        return new JwtAuthenticationToken(jwt, authorities);
    }
}
```

Register in SecurityConfig:
```java
.oauth2ResourceServer(oauth2 -> oauth2
    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtClaimsConverter))
)
```

### 7.8 Adding New Claims

When adding new endpoints/resources:

1. **Define claim**: Follow naming convention `resource.operation`
2. **Create in Keycloak**: Client → Roles → Create role
3. **Update composite roles**: Assign to appropriate roles (Customer, Admin, etc.)
4. **Update mappers** (if needed): Add to client scope mappers
5. **Implement in code**: Add `@PreAuthorize("hasAuthority('new.claim')")`
6. **Update OpenAPI**: Add security requirement to endpoint
7. **Document**: Update `docs/security/operation-claims-guide.md`
8. **Test**: Add positive & negative test cases

### 7.9 Configuration Files

#### Gateway (application.yml)
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
            client-secret: ${KEYCLOAK_CLIENT_SECRET}
            scope: openid,profile,email
            authorization-grant-type: authorization_code
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/ecommerce
  cloud:
    gateway:
      routes:
        - id: order-service
          filters:
            - TokenRelay=
```

#### Backend Services (application.yml)
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/ecommerce
          jwk-set-uri: http://localhost:8180/realms/ecommerce/protocol/openid-connect/certs
```

### 7.10 Testing Security

- Use `@WithMockJwt` for unit tests (custom annotation)
- Integration tests with real Keycloak (Testcontainers)
- Test positive (allowed) and negative (forbidden) cases
- Test unauthenticated access (401)
- Test missing claims (403)
- See: `docs/security/security-testing-guide.md`

### 7.11 References

- **Keycloak Setup**: [docs/security/keycloak-integration.md](docs/security/keycloak-integration.md)
- **Claims Reference**: [docs/security/operation-claims-guide.md](docs/security/operation-claims-guide.md)
- **Token Structure**: [docs/security/jwt-token-structure.md](docs/security/jwt-token-structure.md)
- **Testing Guide**: [docs/security/security-testing-guide.md](docs/security/security-testing-guide.md)
- **Architecture ADR**: [docs/architecture/security-architecture.md](docs/architecture/security-architecture.md)

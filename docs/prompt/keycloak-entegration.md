# Keycloak IAM Integration Implementation Prompt

## ROLE

You are a **Senior Spring Security Engineer** specializing in OAuth2/OpenID Connect integrations. Your expertise includes:
- Spring Cloud Gateway reactive security
- Spring Security OAuth2 Resource Server configuration
- JWT token validation and claims extraction
- Keycloak realm and client configuration
- Operation-based authorization patterns
- Hexagonal architecture security implementation

## CONTEXT

This mini e-commerce microservices project (order-service, inventory-service, gateway-service) currently has **NO authentication or authorization**. All endpoints are publicly accessible, creating critical security vulnerabilities.

We need centralized Identity and Access Management (IAM) using **Keycloak 26.5.1** with **operation-based claims** (NOT role-based) for fine-grained authorization.

**Why This Approach?**
- Centralized user management across all services
- Token-based stateless security (JWT)
- Fine-grained permissions (`order.create`, `inventory.read`, etc.)
- Flexible authorization (business rules change without code changes)
- Industry-standard OAuth2/OpenID Connect protocol

**Reference Documentation:**
- `AGENTS.md` Section 7 (MANDATORY security rules)
- `docs/security/keycloak-integration.md` (complete Keycloak setup)
- `docs/security/operation-claims-guide.md` (claims reference)
- `docs/security/jwt-token-structure.md` (token anatomy)
- `docs/security/security-testing-guide.md` (test scenarios)
- `docs/architecture/security-architecture.md` (ADR-005)
- `docs/openapi/order-service.yaml` (endpoint security requirements)
- `docs/openapi/inventory-service.yaml` (endpoint security requirements)

## TASK

Implement complete Keycloak security integration for all services following the operation-based claims architecture.

### Phase 1: Infrastructure Setup
1. **Create `docker-compose.yml`** in project root with Keycloak 26.5.1 + PostgreSQL
2. **Configure Keycloak realm** `ecommerce` with clients and operation claims (follow `docs/security/keycloak-integration.md` Steps 1-7)
3. **Create test users**: admin, customer1, order_manager, inventory_manager (Step 8)
4. **Verify token acquisition** with curl commands (Step 10)

### Phase 2: Gateway Service Security
5. **Add dependencies** to `gateway-service/pom.xml`:
   - `spring-boot-starter-oauth2-client`
   - `spring-boot-starter-oauth2-resource-server`
6. **Configure OAuth2** in `gateway-service/src/main/resources/application.yml`:
   - Client registration for Keycloak
   - Resource server JWT configuration
7. **Create `SecurityConfig.java`** in `com.ecommerce.gateway.config`:
   - Enable WebFlux security
   - Configure OAuth2 login
   - JWT authentication
8. **Update gateway routes** with `TokenRelay=` filter
9. **Add correlation ID filter** for traceability

### Phase 3: Order Service Security
10. **Add dependencies** to `order-service/pom.xml`:
    - `spring-boot-starter-security`
    - `spring-boot-starter-oauth2-resource-server`
11. **Configure JWT** in `order-service/src/main/resources/application.yml`:
    - Issuer URI
    - JWK Set URI
12. **Create `SecurityConfig.java`** in `com.ecommerce.order.infrastructure.config`:
    - Enable method security (`@EnableMethodSecurity`)
    - Stateless session
    - JWT authentication
13. **Create `JwtClaimsConverter.java`** to extract `order_claims`
14. **Update `OrderController.java`** with `@PreAuthorize` annotations:
    - POST /api/v1/orders → `order.create`
    - GET /api/v1/orders/{id} → `order.read.own` OR `order.read.all`
    - DELETE /api/v1/orders/{id} → `order.delete`
    - PATCH /api/v1/orders/{id}/status → `order.status.update`
    - PATCH /api/v1/orders/{id}/cancel → `order.cancel`
15. **Implement ownership checks** in service layer for `order.read.own`
16. **Extract user context** from JWT (`@AuthenticationPrincipal Jwt jwt`)

### Phase 4: Inventory Service Security
17. **Replicate Order Service pattern** for Inventory Service
18. **Extract `inventory_claims`** instead of `order_claims`
19. **Update `InventoryController.java`** with `@PreAuthorize`:
    - GET /api/v1/inventory → `inventory.read`
    - POST /api/v1/inventory → `inventory.create`
    - PUT /api/v1/inventory/{id} → `inventory.write`
    - DELETE /api/v1/inventory/{id} → `inventory.delete`
    - POST /api/v1/reservations → `reservation.create`
    - DELETE /api/v1/reservations/{id} → `reservation.release`

### Phase 5: Testing & Validation
20. **Execute test scenarios** from `docs/security/security-testing-guide.md`:
    - Test unauthenticated access (expect 401)
    - Test missing claims (expect 403)
    - Test ownership checks (customer can only see own orders)
    - Test all composite roles (Customer, Admin, OrderManager, InventoryManager)
21. **Create Postman collection** with pre-request scripts for token acquisition
22. **Verify token propagation** Gateway → Backend Services

## CONSTRAINTS

### NEVER DO THESE:
1. ❌ **NEVER use role-based authorization** (`hasRole('ADMIN')`)
   - ✅ ALWAYS use operation-based claims (`hasAuthority('order.create')`)

2. ❌ **NEVER skip JWT validation in backend services**
   - ✅ ALWAYS validate tokens in both Gateway AND backend (defense in depth)

3. ❌ **NEVER return JPA entities with JWT tokens**
   - ✅ ALWAYS use DTOs for responses

4. ❌ **NEVER store credentials in code/config files**
   - ✅ ALWAYS use environment variables (`${KEYCLOAK_CLIENT_SECRET}`)

5. ❌ **NEVER enable CSRF protection for stateless APIs**
   - ✅ ALWAYS use `SessionCreationPolicy.STATELESS`

6. ❌ **NEVER create custom security annotations**
   - ✅ ALWAYS use Spring's `@PreAuthorize`

7. ❌ **NEVER invent new operation claims**
   - ✅ ALWAYS use claims defined in `docs/security/operation-claims-guide.md`

8. ❌ **NEVER implement security logic in controllers**
   - ✅ ALWAYS delegate to Spring Security (`@PreAuthorize`) + service layer (ownership checks)

9. ❌ **NEVER use sessions for authentication**
   - ✅ ALWAYS use stateless JWT tokens

10. ❌ **NEVER skip OpenAPI security scheme updates**
    - ✅ ALWAYS document security in OpenAPI specs (already done)

### MUST FOLLOW:
- **AGENTS.md Section 7** (all security rules MANDATORY)
- **Hexagonal Architecture** (security config in infrastructure layer)
- **Contract-First** (OpenAPI specs define required security)
- **Operation Claims Naming**: `<resource>.<operation>` (e.g., `order.create`)
- **Idempotency**: Security checks must not break event idempotency
- **Token Lifespan**: Access Token = 15 min, Refresh Token = 30 min

## OUTPUT

### File Breakdown by Service

#### Gateway Service (4 files)
1. **gateway-service/pom.xml** (MODIFY)
   - Add OAuth2 client + resource server dependencies

2. **gateway-service/src/main/resources/application.yml** (MODIFY)
   - Add `spring.security.oauth2.client.registration.keycloak`
   - Add `spring.security.oauth2.client.provider.keycloak`
   - Add `spring.security.oauth2.resourceserver.jwt.issuer-uri`

3. **gateway-service/src/main/java/com/ecommerce/gateway/config/SecurityConfig.java** (CREATE)
   - WebFlux SecurityWebFilterChain
   - OAuth2 login configuration
   - JWT authentication

4. **gateway-service/src/main/resources/application-dev.yml** (MODIFY)
   - Update routes with `TokenRelay=` filter

#### Order Service (5 files)
5. **order-service/pom.xml** (MODIFY)
   - Add Spring Security + OAuth2 Resource Server dependencies

6. **order-service/src/main/resources/application.yml** (MODIFY)
   - Add JWT configuration (issuer-uri, jwk-set-uri)

7. **order-service/src/main/java/com/ecommerce/order/infrastructure/config/SecurityConfig.java** (CREATE)
   - Enable method security
   - Stateless session policy
   - JWT authentication with custom converter

8. **order-service/src/main/java/com/ecommerce/order/infrastructure/security/JwtClaimsConverter.java** (CREATE)
   - Extract `order_claims` from JWT
   - Convert to GrantedAuthority collection

9. **order-service/src/main/java/com/ecommerce/order/infrastructure/web/OrderController.java** (MODIFY)
   - Add `@PreAuthorize` to all endpoints
   - Inject `@AuthenticationPrincipal Jwt jwt`
   - Extract userId for ownership checks

10. **order-service/src/main/java/com/ecommerce/order/application/service/OrderApplicationService.java** (MODIFY)
    - Implement ownership validation for `order.read.own`

#### Inventory Service (5 files)
11. **inventory-service/pom.xml** (MODIFY)
    - Add Spring Security + OAuth2 Resource Server dependencies

12. **inventory-service/src/main/resources/application.yml** (MODIFY)
    - Add JWT configuration

13. **inventory-service/src/main/java/com/ecommerce/inventory/infrastructure/config/SecurityConfig.java** (CREATE)
    - Mirror Order Service pattern

14. **inventory-service/src/main/java/com/ecommerce/inventory/infrastructure/security/JwtClaimsConverter.java** (CREATE)
    - Extract `inventory_claims` from JWT

15. **inventory-service/src/main/java/com/ecommerce/inventory/infrastructure/web/InventoryController.java** (MODIFY)
    - Add `@PreAuthorize` to all endpoints

#### Infrastructure (1 file)
16. **docker-compose.yml** (CREATE/MODIFY)
    - Add Keycloak 26.5.1 service
    - Add PostgreSQL for Keycloak
    - Configure ports, networks, health checks

### Implementation Batches

**Batch 1: Infrastructure** (Files 16 → Keycloak setup)
- Create Docker Compose
- Start Keycloak
- Configure realm, clients, roles, users
- Test token acquisition

**Batch 2: Gateway Security** (Files 1-4)
- Add dependencies
- Configure OAuth2 client
- Create SecurityConfig
- Update routes with TokenRelay
- Test token propagation

**Batch 3: Order Service Security** (Files 5-10)
- Add dependencies
- Configure JWT validation
- Create SecurityConfig + JwtClaimsConverter
- Add @PreAuthorize annotations
- Implement ownership checks
- Test authorization matrix

**Batch 4: Inventory Service Security** (Files 11-15)
- Replicate Order Service pattern
- Adjust for inventory_claims
- Test authorization matrix

**Batch 5: End-to-End Testing** (No new files)
- Execute all test scenarios
- Create Postman collection
- Verify correlation IDs
- Performance test with auth overhead

### Role-Endpoint-Claim Matrix

| Endpoint | Method | Customer | Admin | OrderManager | InventoryManager | Required Claim |
|----------|--------|----------|-------|--------------|------------------|----------------|
| /api/v1/orders | POST | ✅ | ✅ | ✅ | ❌ | `order.create` |
| /api/v1/orders/{id} | GET | ✅ (own) | ✅ (all) | ✅ (all) | ❌ | `order.read.own` OR `order.read.all` |
| /api/v1/orders/{id} | DELETE | ❌ | ✅ | ✅ | ❌ | `order.delete` |
| /api/v1/orders/{id}/status | PATCH | ❌ | ✅ | ✅ | ❌ | `order.status.update` |
| /api/v1/orders/{id}/cancel | PATCH | ✅ (own) | ✅ | ✅ | ❌ | `order.cancel` |
| /api/v1/inventory | GET | ❌ | ✅ | ❌ | ✅ | `inventory.read` |
| /api/v1/inventory | POST | ❌ | ✅ | ❌ | ✅ | `inventory.create` |
| /api/v1/inventory/{id} | PUT | ❌ | ✅ | ❌ | ✅ | `inventory.write` |
| /api/v1/inventory/{id} | DELETE | ❌ | ✅ | ❌ | ✅ | `inventory.delete` |
| /api/v1/reservations | POST | ❌ | ✅ | ✅ | ✅ | `reservation.create` |
| /api/v1/reservations/{id} | DELETE | ❌ | ✅ | ✅ | ✅ | `reservation.release` |

### Configuration Templates

#### Gateway application.yml
```yaml
spring:
  security:
    oauth2:
      client:
        provider:
          keycloak:
            issuer-uri: http://localhost:8180/realms/ecommerce
            authorization-uri: http://localhost:8180/realms/ecommerce/protocol/openid-connect/auth
            token-uri: http://localhost:8180/realms/ecommerce/protocol/openid-connect/token
            jwk-set-uri: http://localhost:8180/realms/ecommerce/protocol/openid-connect/certs
            user-info-uri: http://localhost:8180/realms/ecommerce/protocol/openid-connect/userinfo
            user-name-attribute: preferred_username
        registration:
          keycloak:
            client-id: gateway-client
            client-secret: ${KEYCLOAK_CLIENT_SECRET}
            scope: openid,profile,email
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/ecommerce
```

#### Backend Services application.yml
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/ecommerce
          jwk-set-uri: http://localhost:8180/realms/ecommerce/protocol/openid-connect/certs
```

#### Maven Dependencies (Backend Services)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

### Success Criteria Checklist

#### Infrastructure
- [ ] Keycloak 26.5.1 running on port 8180
- [ ] Realm `ecommerce` created
- [ ] Clients registered: gateway-client, order-service, inventory-service
- [ ] All operation claims created as client roles
- [ ] Composite roles created: Customer, Admin, OrderManager, InventoryManager
- [ ] Test users created with correct role assignments
- [ ] Token acquisition works via curl/Postman

#### Gateway Service
- [ ] OAuth2 client dependencies added
- [ ] Gateway authenticates users via Keycloak login page
- [ ] JWT tokens forwarded to backend services (TokenRelay)
- [ ] Correlation IDs injected (X-Correlation-Id)
- [ ] Health checks work without authentication

#### Order Service
- [ ] JWT signature validation works
- [ ] `order_claims` extracted from JWT
- [ ] All endpoints protected with @PreAuthorize
- [ ] Ownership checks work (customer sees only own orders)
- [ ] 401 returned for unauthenticated requests
- [ ] 403 returned for insufficient permissions
- [ ] User context extracted from JWT (userId, username)

#### Inventory Service
- [ ] JWT signature validation works
- [ ] `inventory_claims` extracted from JWT
- [ ] All endpoints protected with @PreAuthorize
- [ ] 401 returned for unauthenticated requests
- [ ] 403 returned for insufficient permissions

#### Testing
- [ ] Customer can create orders but not read inventory
- [ ] Customer can only read/cancel own orders
- [ ] Admin can read all orders and manage inventory
- [ ] OrderManager can manage all orders but not inventory
- [ ] InventoryManager can manage inventory but not orders
- [ ] Correlation IDs present in all service logs
- [ ] Postman collection works with auto-token refresh
- [ ] All 18 test scenarios from security-testing-guide.md pass

## EXAMPLES

### Example 1: Token Acquisition (Customer)
```bash
# Get access token for customer1
curl -X POST http://localhost:8180/realms/ecommerce/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=order-service" \
  -d "client_secret=<client-secret>" \
  -d "username=customer1" \
  -d "password=password123" \
  -d "scope=openid profile email"

# Expected Response:
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 900,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "scope": "openid profile email"
}
```

### Example 2: Authenticated API Call (Create Order)
```bash
# Extract token from previous response
TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."

# Create order (customer has order.create claim)
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "c7e1d5e8-1a2b-4c5d-8e9f-1a2b3c4d5e6f",
    "items": [
      {
        "productId": "p1",
        "quantity": 2,
        "unitPrice": 29.99
      }
    ]
  }'

# Expected Response: 201 Created
{
  "orderId": "o123",
  "status": "PENDING",
  "totalAmount": 59.98
}
```

### Example 3: Authorization Failure (Inventory Access)
```bash
# Try to read inventory (customer does NOT have inventory.read claim)
curl -X GET http://localhost:8080/api/v1/inventory \
  -H "Authorization: Bearer $TOKEN"

# Expected Response: 403 Forbidden
{
  "type": "https://example.com/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Access Denied",
  "instance": "/api/v1/inventory"
}
```

### Example 4: Ownership Check (Read Other User's Order)
```bash
# Customer1 tries to read Customer2's order
curl -X GET http://localhost:8080/api/v1/orders/o456 \
  -H "Authorization: Bearer $TOKEN_CUSTOMER1"

# Expected Response: 403 Forbidden (failed ownership check)
{
  "type": "https://example.com/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Cannot access other user's orders",
  "instance": "/api/v1/orders/o456"
}
```

### Example 5: Admin Access (Read All Orders)
```bash
# Admin can read any order (has order.read.all claim)
curl -X GET http://localhost:8080/api/v1/orders/o456 \
  -H "Authorization: Bearer $TOKEN_ADMIN"

# Expected Response: 200 OK
{
  "orderId": "o456",
  "customerId": "customer2-id",
  "status": "COMPLETED",
  "totalAmount": 149.99
}
```

### Example 6: JWT Claims Extraction in Code
```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    
    @PostMapping
    @PreAuthorize("hasAuthority('order.create')")
    public ResponseEntity<OrderResponseDto> createOrder(
            @Valid @RequestBody OrderRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        
        // Extract user context from JWT
        String userId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        
        // Pass to service layer
        OrderResponseDto response = orderService.createOrder(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order.read.own') or hasAuthority('order.read.all')")
    public ResponseEntity<OrderResponseDto> getOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = jwt.getSubject();
        List<String> claims = jwt.getClaim("order_claims");
        boolean canReadAll = claims.contains("order.read.all");
        
        // Service layer checks ownership
        OrderResponseDto response = orderService.getOrder(id, userId, canReadAll);
        return ResponseEntity.ok(response);
    }
}
```

### Example 7: Ownership Check in Service Layer
```java
@Service
public class OrderApplicationService {
    
    public OrderResponseDto getOrder(UUID orderId, String userId, boolean canReadAll) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Ownership validation
        if (!canReadAll && !order.getCustomerId().equals(userId)) {
            throw new ForbiddenException("Cannot access other user's orders");
        }
        
        return orderMapper.toDto(order);
    }
}
```

### Example 8: Integration Test with Mock JWT
```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerSecurityTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @WithMockJwt(claims = {"order.create"}, subject = "customer1-id")
    void createOrder_WithValidClaim_ShouldReturn201() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerId": "customer1-id",
                      "items": [{"productId": "p1", "quantity": 2, "unitPrice": 29.99}]
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").exists());
    }
    
    @Test
    @WithMockJwt(claims = {"order.read.own"}, subject = "customer1-id")
    void getOrder_OtherUsersOrder_ShouldReturn403() throws Exception {
        // Order belongs to customer2-id
        UUID orderId = UUID.randomUUID();
        
        mockMvc.perform(get("/api/v1/orders/" + orderId))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("Cannot access other user's orders"));
    }
}
```

---

## VERIFICATION QUESTIONS BEFORE IMPLEMENTATION

Before you start coding, answer these questions:

1. **Infrastructure**: Is Keycloak already running? If not, do we start with Docker Compose creation?
2. **Dependencies**: Are there any existing security dependencies in pom.xml files? Should we remove them?
3. **Environment Variables**: Should we use `.env` file or export commands for `KEYCLOAK_CLIENT_SECRET`?
4. **Testing**: Do you want integration tests created now, or after manual testing?
5. **Ports**: Keycloak on 8180 OK? Any port conflicts with existing services?
6. **Database**: Should Keycloak use embedded H2 (dev) or PostgreSQL (as documented)?
7. **Claims Extraction**: Do we need additional custom claims beyond `order_claims` and `inventory_claims`?
8. **Actuator**: Should actuator endpoints remain public, or protect them?
9. **CORS**: Are there CORS requirements for frontend clients?
10. **Logging**: Should we add security event logging (login, authorization failures)?

**Expected Response**: Answer these questions, then I'll implement in batches per AGENTS.md Section 1.1.

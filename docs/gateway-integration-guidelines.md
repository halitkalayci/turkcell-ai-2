# Gateway Integration Guidelines

**Document Type:** Architecture Guidelines  
**Last Updated:** 2026-01-22  
**Applies To:** All Microservices in E-Commerce Platform

---

## Purpose

This document defines **mandatory rules** for integrating new microservices with the API Gateway. All developers and AI agents MUST follow these guidelines when adding new services to the platform.

---

## Overview

Our e-commerce platform uses **Spring Cloud Gateway** as the single entry point for all client requests. The gateway handles:

- Request routing to downstream services
- Request/response logging with correlation IDs
- Error handling and transformation
- Health monitoring of downstream services
- CORS configuration

**Gateway Port:** `8080`  
**Gateway Base URL:** `http://localhost:8080`

---

## Integration Checklist for New Services

When adding a new microservice (e.g., `payment-service`, `notification-service`):

### 1. Service Prerequisites

- [ ] Service has an OpenAPI contract in `docs/openapi/{service-name}.yaml`
- [ ] Service follows `/api/v1` versioning prefix (ADR-002)
- [ ] Service exposes `/actuator/health` endpoint
- [ ] Service is assigned a unique port (8081+)
- [ ] Service follows hexagonal architecture (AGENTS.md)

### 2. Gateway Route Configuration

Add route configuration to `gateway-service/src/main/resources/application.yml`:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: {service-name}
          uri: http://localhost:{port}
          predicates:
            - Path=/api/v1/{resource-path}/**
          filters:
            - name: RequestLogging
            - name: ResponseLogging
```

**Rules:**
- Route `id` must be unique and match service name
- `uri` points to service's base URL (without `/api/v1`)
- Path predicate matches the resource path from OpenAPI contract
- Always include `RequestLogging` and `ResponseLogging` filters
- Do NOT strip the `/api/v1` prefix (set `stripPrefix: false` or omit)

### 3. Health Check Integration

Add custom health indicator in `gateway-service/src/main/java/com/ecommerce/gateway/config/`:

```java
@Component
public class {ServiceName}HealthIndicator implements HealthIndicator {
    
    private final WebClient webClient;
    
    public {ServiceName}HealthIndicator(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:{port}").build();
    }
    
    @Override
    public Health health() {
        try {
            String response = webClient.get()
                .uri("/actuator/health")
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(5));
            
            return Health.up()
                .withDetail("uri", "http://localhost:{port}/actuator/health")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### 4. Documentation Updates

- [ ] Add service to gateway's `README.md` routes table
- [ ] Add curl examples for new service endpoints
- [ ] Update main project `README.md` with new service info
- [ ] Document any new filters or custom configurations

### 5. Port Assignment

Follow the port assignment strategy:

| Port | Service |
|------|---------|
| 8080 | Gateway (main entry point) |
| 8081 | Order Service |
| 8082 | Inventory Service |
| 8083+ | New services (assign incrementally) |

**Rule:** Never reuse ports. Always use the next available port.

---

## Routing Patterns

### Pattern 1: Single Resource Path

Service has one main resource:

```yaml
# Example: Order Service
# Endpoints: /api/v1/orders, /api/v1/orders/{id}
routes:
  - id: order-service
    uri: http://localhost:8081
    predicates:
      - Path=/api/v1/orders/**
```

### Pattern 2: Multiple Resource Paths

Service has multiple distinct resources:

```yaml
# Example: Inventory Service
# Resources: inventory-items, reservations
routes:
  - id: inventory-items
    uri: http://localhost:8082
    predicates:
      - Path=/api/v1/inventory-items/**
  
  - id: reservations
    uri: http://localhost:8082
    predicates:
      - Path=/api/v1/reservations/**
```

**Rule:** Create separate route entries for each top-level resource path to maintain clarity.

---

## Error Handling Rules

Gateway returns standardized RFC 7807 Problem Details for errors:

### Downstream Service Errors

| Scenario | Gateway Status | Problem Details |
|----------|---------------|-----------------|
| Service down | 502 Bad Gateway | `ConnectException` caught |
| Service timeout | 504 Gateway Timeout | Request exceeds 30s |
| Service error (4xx/5xx) | Pass-through | Original error forwarded |

### Gateway Internal Errors

| Scenario | Gateway Status | Problem Details |
|----------|---------------|-----------------|
| Route not found | 404 Not Found | No matching route |
| Gateway error | 500 Internal Server Error | Gateway internal failure |

**Rule:** Gateway MUST NOT modify error responses from services unless the service is unreachable.

---

## Logging and Tracing

### Correlation ID

Gateway automatically injects `X-Correlation-Id` header to all requests:

```
X-Correlation-Id: 550e8400-e29b-41d4-a716-446655440000
```

**Downstream services MUST:**
- Preserve this header in all outgoing requests
- Include it in log messages for traceability
- Return it in error responses

### Request Logging Format

```
[GATEWAY] [correlation-id] [method] [path] -> [target-uri] [duration-ms]
```

Example:
```
[GATEWAY] [550e8400...] POST /api/v1/orders -> http://localhost:8081/api/v1/orders [145ms]
```

---

## Testing New Service Integration

### Manual Testing Steps

1. **Direct Access Test**
   ```bash
   # Verify service works directly
   curl http://localhost:{port}/api/v1/{resource}
   ```

2. **Gateway Access Test**
   ```bash
   # Verify routing through gateway
   curl http://localhost:8080/api/v1/{resource}
   ```

3. **Health Check Test**
   ```bash
   # Verify gateway detects service health
   curl http://localhost:8080/actuator/health
   ```

4. **Error Handling Test**
   ```bash
   # Stop service, verify 502 Bad Gateway
   curl http://localhost:8080/api/v1/{resource}
   ```

5. **Correlation ID Test**
   ```bash
   # Verify X-Correlation-Id in response headers
   curl -v http://localhost:8080/api/v1/{resource}
   ```

### Success Criteria

- [ ] Service accessible via gateway on port 8080
- [ ] Health check shows service as UP
- [ ] Correlation ID present in logs
- [ ] Error handling returns proper status codes
- [ ] Response time acceptable (<1s for health checks)

---

## Common Pitfalls

### ❌ DON'T: Strip API Version Prefix

```yaml
# WRONG - removes /api/v1
routes:
  - id: my-service
    uri: http://localhost:8083
    predicates:
      - Path=/api/v1/my-resource/**
    filters:
      - StripPrefix=2  # ❌ This removes /api/v1
```

Services expect `/api/v1` in their paths (ADR-002). Gateway MUST preserve it.

### ❌ DON'T: Hard-code Hostnames

```yaml
# WRONG - not flexible for deployment
uri: http://order-service.prod.internal:8081
```

Use `localhost` for development. Environment-specific URIs will be configured via Spring Cloud Config or environment variables in production.

### ❌ DON'T: Create Overlapping Routes

```yaml
# WRONG - ambiguous routing
routes:
  - id: route1
    predicates:
      - Path=/api/v1/**  # Too broad
  - id: route2
    predicates:
      - Path=/api/v1/orders/**  # Will never match
```

More specific routes MUST be defined first, or use unique resource paths.

### ✅ DO: Use Specific Paths

```yaml
# CORRECT
routes:
  - id: orders
    predicates:
      - Path=/api/v1/orders/**
  - id: inventory
    predicates:
      - Path=/api/v1/inventory-items/**
```

---

## Configuration Management

### Development (application-dev.yml)

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/v1/orders/**
```

### Production (Future: Spring Cloud Config)

In production, URIs will be resolved via service discovery:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service  # Load-balanced via service registry
          predicates:
            - Path=/api/v1/orders/**
```

**Current Status:** Service discovery NOT implemented. Use direct URIs for now.

---

## Contract-First Compliance

Gateway routing MUST align with OpenAPI contracts:

1. **Read the Contract First**
   - Check `docs/openapi/{service-name}.yaml`
   - Identify all path patterns
   - Note the base path (must be `/api/v1`)

2. **Map Routes Exactly**
   - Gateway paths MUST match contract paths
   - Do NOT invent new paths or modify existing ones

3. **Validate Against Contract**
   - Test that gateway forwards requests correctly
   - Verify response schemas match contract
   - Ensure error responses follow contract error models

---

## Security Considerations (Future)

Gateway will be the enforcement point for:

- Authentication (JWT validation)
- Authorization (role-based access)
- Rate limiting
- Request throttling

**Current Status:** No security implemented. This is a future enhancement.

---

## References

- `AGENTS.md` - Architecture and coding standards
- `DECISIONS.md` - ADR-002 (API Versioning)
- `docs/openapi/` - Service contracts
- [Spring Cloud Gateway Documentation](https://spring.io/projects/spring-cloud-gateway)
- [RFC 7807 Problem Details](https://www.rfc-editor.org/rfc/rfc7807)

---

## Questions?

If any rule is unclear or conflicts with existing architecture:

1. Check `AGENTS.md` and `DECISIONS.md` first
2. Review existing service implementations
3. Ask for clarification rather than making assumptions

**AI Agents:** Do NOT invent solutions. Follow this document strictly.

# Task 02: Configure Gateway Routes for Services

**Status:** Not Started  
**Priority:** High  
**Dependencies:** Task 01  
**Estimated Time:** 45 minutes

---

## Objective

Configure Spring Cloud Gateway routing rules for all existing microservices based on OpenAPI contracts.

---

## Requirements

1. Implement routing rules for **Order Service** (port 8081)
2. Implement routing rules for **Inventory Service** (port 8082)
3. Maintain `/api/v1` versioning prefix for all routes
4. Configure request/response logging filters
5. Add global timeout configurations
6. Implement basic error handling

---

## Routing Strategy

### Current Service Landscape (from OpenAPI contracts)

#### Order Service (localhost:8081)
- Base path: `/api/v1`
- Endpoints:
  - `POST /api/v1/orders`
  - `GET /api/v1/orders/{id}`
  - `PUT /api/v1/orders/{id}/cancel`
  - `PUT /api/v1/orders/{id}/status`
  - `GET /api/v1/orders`

#### Inventory Service (localhost:8082)
- Base path: `/api/v1`
- Endpoints:
  - `GET /api/v1/inventory-items/{productId}`
  - `POST /api/v1/inventory-items/availability-check`
  - `POST /api/v1/reservations`
  - `PUT /api/v1/reservations/{reservationId}/confirm`
  - `PUT /api/v1/reservations/{reservationId}/release`
  - `GET /api/v1/reservations/{reservationId}`

---

## Gateway Routing Rules

### Rule 1: Order Service Routes
```yaml
# Client Request: http://localhost:8080/api/v1/orders/**
# Gateway forwards to: http://localhost:8081/api/v1/orders/**
```

**Path Pattern:** `/api/v1/orders/**`  
**Target URI:** `http://localhost:8081`  
**Strip Prefix:** `false` (we keep `/api/v1/orders` intact)

### Rule 2: Inventory Service Routes
```yaml
# Client Request: http://localhost:8080/api/v1/inventory-items/**
# Gateway forwards to: http://localhost:8082/api/v1/inventory-items/**

# Client Request: http://localhost:8080/api/v1/reservations/**
# Gateway forwards to: http://localhost:8082/api/v1/reservations/**
```

**Path Pattern:** `/api/v1/inventory-items/**`  
**Target URI:** `http://localhost:8082`  
**Strip Prefix:** `false`

**Path Pattern:** `/api/v1/reservations/**`  
**Target URI:** `http://localhost:8082`  
**Strip Prefix:** `false`

---

## Filters to Implement

### 1. Request Logging Filter
- Log incoming request method, path, and headers (DEBUG level)
- Add request timestamp
- Add correlation ID (X-Correlation-Id header)

### 2. Response Logging Filter
- Log response status code and duration
- Log correlation ID for traceability

### 3. Error Handling Filter
- Catch downstream service errors
- Transform to RFC 7807 Problem Details format
- Return appropriate HTTP status codes:
  - 502 Bad Gateway: Service is down
  - 504 Gateway Timeout: Service timeout
  - 500 Internal Server Error: Gateway internal errors

---

## Checklist

### Configuration Files

- [ ] Update `application.yml` with route configurations
- [ ] Add route for Order Service
- [ ] Add route for Inventory Items endpoint
- [ ] Add route for Reservations endpoints
- [ ] Configure global timeout: 30 seconds
- [ ] Configure connection timeout: 5 seconds

### Java Configuration (GatewayConfig.java)

- [ ] Create `RouteLocator` bean for programmatic route definition
- [ ] Add predicates for path-based routing
- [ ] Add filters for logging
- [ ] Add filters for correlation ID injection
- [ ] Configure retry logic (optional, 2 retries with exponential backoff)

### Logging Filter Implementation

- [ ] Create `RequestLoggingFilter.java`
- [ ] Implement `GlobalFilter` interface
- [ ] Log request method + path
- [ ] Generate and inject `X-Correlation-Id` if missing
- [ ] Set filter order: `-1` (execute before routing)

### Error Handler Implementation

- [ ] Create `GlobalErrorHandler.java`
- [ ] Implement `ErrorWebExceptionHandler` interface
- [ ] Handle `ConnectException` → 502 Bad Gateway
- [ ] Handle `TimeoutException` → 504 Gateway Timeout
- [ ] Handle generic errors → 500 Internal Server Error
- [ ] Return RFC 7807 Problem Details JSON

---

## Testing Strategy

### Manual Testing Checklist

1. **Order Service via Gateway**
   - [ ] `POST http://localhost:8080/api/v1/orders` returns 201
   - [ ] `GET http://localhost:8080/api/v1/orders/{id}` returns 200
   - [ ] Verify logs show request correlation ID

2. **Inventory Service via Gateway**
   - [ ] `GET http://localhost:8080/api/v1/inventory-items/{productId}` returns 200
   - [ ] `POST http://localhost:8080/api/v1/reservations` returns 201
   - [ ] Verify logs show request correlation ID

3. **Error Handling**
   - [ ] Stop Order Service, request returns 502 Bad Gateway
   - [ ] Stop Inventory Service, request returns 502 Bad Gateway
   - [ ] Verify error response is RFC 7807 compliant

4. **Direct Service Access (Should Still Work)**
   - [ ] `GET http://localhost:8081/api/v1/orders` (direct access)
   - [ ] `GET http://localhost:8082/api/v1/inventory-items/{productId}` (direct access)

---

## Configuration Example Structure

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/v1/orders/**
          filters:
            - name: RequestLogging
            - name: ResponseLogging
            
        - id: inventory-items
          uri: http://localhost:8082
          predicates:
            - Path=/api/v1/inventory-items/**
          filters:
            - name: RequestLogging
            - name: ResponseLogging
            
        - id: reservations
          uri: http://localhost:8082
          predicates:
            - Path=/api/v1/reservations/**
          filters:
            - name: RequestLogging
            - name: ResponseLogging
      
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
            allowedHeaders: "*"
```

---

## Validation Criteria

- [ ] All routes defined in checklist are accessible via gateway
- [ ] Logs show correlation IDs for all requests
- [ ] Error responses follow RFC 7807 format
- [ ] Gateway returns 502 when services are down
- [ ] Response times logged correctly
- [ ] `/api/v1` prefix maintained in all forwarded requests

---

## Contract-First Compliance

✅ Routes match OpenAPI contract paths exactly  
✅ Gateway preserves `/api/v1` versioning (ADR-002)  
✅ No business logic in gateway (AGENTS.md compliance)  

---

## References

- docs/openapi/order-service.yaml
- docs/openapi/inventory-service.yaml
- [Spring Cloud Gateway Filters](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#gateway-request-predicates-factories)
- [RFC 7807 Problem Details](https://www.rfc-editor.org/rfc/rfc7807)

# Gateway Service

## Overview

The **Gateway Service** is the single entry point for all client requests in the E-Commerce Microservices Platform. It provides intelligent routing, distributed tracing, observability, and error handling for downstream services.

Built with **Spring Cloud Gateway** (reactive, WebFlux-based), it runs on port **8080** and routes requests to:
- **Order Service** (port 8081)
- **Inventory Service** (port 8082)

## Architecture

```
┌─────────────┐
│   Clients   │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────┐
│      Gateway Service (8080)          │
│  ┌────────────────────────────────┐  │
│  │  Correlation ID Injection      │  │
│  │  Request/Response Logging      │  │
│  │  Error Handling (RFC 7807)     │  │
│  │  Health Monitoring             │  │
│  └────────────────────────────────┘  │
└──────┬──────────────────────┬───────┘
       │                      │
       ▼                      ▼
┌─────────────┐      ┌─────────────────┐
│Order Service│      │Inventory Service│
│   (8081)    │      │     (8082)      │
└─────────────┘      └─────────────────┘
```

### Key Features

✅ **Intelligent Routing** - Path-based routing to appropriate services  
✅ **Distributed Tracing** - Auto-generated correlation IDs (X-Correlation-Id)  
✅ **Request/Response Logging** - Complete request lifecycle tracking  
✅ **Health Monitoring** - Real-time downstream service health checks  
✅ **Error Handling** - RFC 7807 Problem Details for gateway errors  
✅ **CORS Support** - Configurable cross-origin access (dev mode enabled)  
✅ **Observability** - Actuator endpoints for metrics and monitoring

---

## Quick Start

### Prerequisites

- **Java 21** or higher
- **Maven 3.8+**
- **Order Service** running on port 8081
- **Inventory Service** running on port 8082

### Build & Run

```bash
# Build the project
mvn clean install

# Run with default profile
mvn spring-boot:run

# Run with development profile (recommended)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The gateway will start on **http://localhost:8080**

### Verify Gateway is Running

```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Expected response:
{
  "status": "UP",
  "components": {
    "orderService": {
      "status": "UP",
      "details": {
        "uri": "http://localhost:8081/actuator/health",
        "status": "Service is reachable"
      }
    },
    "inventoryService": {
      "status": "UP",
      "details": {
        "uri": "http://localhost:8082/actuator/health",
        "status": "Service is reachable"
      }
    }
  }
}
```

---

## Routes Configuration

All routes preserve the `/api/v1` prefix as per ADR-002 (API Versioning).

| Client Request | Target Service | Port | Description |
|----------------|----------------|------|-------------|
| `GET/POST /api/v1/orders/**` | Order Service | 8081 | Order management operations |
| `GET/POST /api/v1/inventory-items/**` | Inventory Service | 8082 | Stock information and availability |
| `GET/POST/PUT /api/v1/reservations/**` | Inventory Service | 8082 | Stock reservation management |

### Route Details

#### Order Service Routes
- `POST /api/v1/orders` - Create new order
- `GET /api/v1/orders/{id}` - Get order by ID
- `GET /api/v1/orders` - List all orders

#### Inventory Service Routes
- `GET /api/v1/inventory-items/{productId}` - Get inventory item
- `POST /api/v1/inventory-items/availability-check` - Check stock availability
- `POST /api/v1/reservations` - Create reservation
- `GET /api/v1/reservations/{id}` - Get reservation by ID
- `PUT /api/v1/reservations/{id}/confirm` - Confirm reservation
- `PUT /api/v1/reservations/{id}/release` - Release reservation

---

## Testing Examples

### 1. Create Order via Gateway

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "123e4567-e89b-12d3-a456-426614174000",
    "items": [
      {
        "productId": "550e8400-e29b-41d4-a716-446655440000",
        "quantity": 2,
        "unitPrice": 29.99
      }
    ]
  }'

# Expected: 201 Created with order details
# Gateway logs will show:
# [GATEWAY-REQUEST] [<correlation-id>] POST /api/v1/orders
# [GATEWAY-RESPONSE] [<correlation-id>] 201 145ms
```

### 2. Check Inventory via Gateway

```bash
curl http://localhost:8080/api/v1/inventory-items/550e8400-e29b-41d4-a716-446655440000

# Expected: 200 OK with inventory details
```

### 3. Create Reservation via Gateway

```bash
curl -X POST http://localhost:8080/api/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "550e8400-e29b-41d4-a716-446655440000",
    "quantity": 5,
    "reservedFor": "ORDER-12345"
  }'

# Expected: 201 Created with reservation details
```

### 4. Test Error Handling (Service Down)

```bash
# Stop order-service, then request:
curl -v http://localhost:8080/api/v1/orders

# Expected: 502 Bad Gateway with RFC 7807 Problem Details
{
  "type": "about:blank",
  "title": "Bad Gateway",
  "status": 502,
  "detail": "Service is currently unavailable",
  "instance": "/api/v1/orders"
}
```

### 5. Test Correlation ID

```bash
# Send request with custom correlation ID
curl -H "X-Correlation-Id: my-custom-id-123" \
  http://localhost:8080/api/v1/orders

# Check gateway logs - should show:
# [GATEWAY-REQUEST] [my-custom-id-123] GET /api/v1/orders
```

---

## Health Checks & Monitoring

### Health Endpoint

```bash
# Check overall health
curl http://localhost:8080/actuator/health

# Check liveness probe
curl http://localhost:8080/actuator/health/liveness

# Check readiness probe
curl http://localhost:8080/actuator/health/readiness
```

### Info Endpoint

```bash
curl http://localhost:8080/actuator/info

# Returns application metadata:
{
  "app": {
    "name": "Gateway Service",
    "description": "API Gateway for E-Commerce Microservices",
    "version": "1.0.0"
  },
  "java": {
    "version": "21"
  }
}
```

### Metrics

```bash
# List all available metrics
curl http://localhost:8080/actuator/metrics

# Gateway-specific metrics
curl http://localhost:8080/actuator/metrics/gateway.requests

# View all configured routes
curl http://localhost:8080/actuator/gateway/routes | jq
```

---

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Gateway server port | `8080` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | (none) |
| `ORDER_SERVICE_URL` | Order Service base URL | `http://localhost:8081` |
| `INVENTORY_SERVICE_URL` | Inventory Service base URL | `http://localhost:8082` |

### Profiles

#### Default Profile
- Basic configuration
- Standard logging levels
- No CORS configured

#### Development Profile (`dev`)
- Verbose logging (DEBUG level)
- CORS enabled for all origins
- Extended timeouts
- Request/response body logging

Activate with:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# OR
java -jar target/gateway-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### Timeout Configuration

| Setting | Default | Dev Profile |
|---------|---------|-------------|
| Connection Timeout | 5s | 5s |
| Response Timeout | 30s | 30s |
| Health Check Timeout | 5s | 5s |

---

## Troubleshooting

### Port 8080 Already in Use

**Problem:** Gateway fails to start with "Port 8080 already in use"

**Solutions:**
```bash
# Option 1: Kill process using port 8080 (Windows)
netstat -ano | findstr :8080
taskkill /PID <process-id> /F

# Option 2: Change gateway port
mvn spring-boot:run -Dserver.port=8090

# Option 3: Set environment variable
$env:SERVER_PORT=8090
mvn spring-boot:run
```

### Downstream Services Not Running (502 Errors)

**Problem:** Gateway returns 502 Bad Gateway errors

**Diagnosis:**
```bash
# Check which services are down
curl http://localhost:8080/actuator/health

# Test services directly
curl http://localhost:8081/actuator/health  # Order Service
curl http://localhost:8082/actuator/health  # Inventory Service
```

**Solution:** Start the required downstream services:
```bash
cd order-service && mvn spring-boot:run &
cd inventory-service && mvn spring-boot:run &
```

### CORS Issues in Development

**Problem:** Browser shows CORS policy errors

**Solution:** Ensure development profile is active:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Verify CORS headers in response:
```bash
curl -v -X OPTIONS http://localhost:8080/api/v1/orders \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST"

# Look for these headers in response:
# Access-Control-Allow-Origin: *
# Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
```

### Gateway Timeout (504 Errors)

**Problem:** Requests timeout after 30 seconds

**Diagnosis:** Check if downstream service is slow to respond

**Solution:** Adjust timeout in `application-dev.yml`:
```yaml
spring:
  cloud:
    gateway:
      httpclient:
        response-timeout: 60s  # Increase to 60 seconds
```

### Missing Correlation IDs in Logs

**Problem:** Logs don't show correlation IDs

**Solution:** Ensure logging level is DEBUG:
```yaml
logging:
  level:
    com.ecommerce.gateway: DEBUG
```

Verify filters are loaded:
```bash
# Check gateway routes and filters
curl http://localhost:8080/actuator/gateway/routes | jq
```

### Health Checks Always Show DOWN

**Problem:** Services appear DOWN even when running

**Diagnosis:**
```bash
# Test health endpoint directly
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health

# Check if Actuator is enabled in services
# Ensure services have spring-boot-starter-actuator dependency
```

**Solution:** Enable Actuator in downstream services' `application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

---

## Logging

### Log Levels

```yaml
logging:
  level:
    org.springframework.cloud.gateway: DEBUG      # Gateway routing logs
    reactor.netty: DEBUG                           # Netty/WebFlux logs
    com.ecommerce.gateway: DEBUG                   # Application logs
```

### Log Format

**Request Logs:**
```
[GATEWAY-REQUEST] [correlation-id] [METHOD] [path]
Example: [GATEWAY-REQUEST] [550e8400-...] POST /api/v1/orders
```

**Response Logs:**
```
[GATEWAY-RESPONSE] [correlation-id] [status-code] [duration-ms]
Example: [GATEWAY-RESPONSE] [550e8400-...] 201 145ms
```

**Error Logs:**
```
[GATEWAY-ERROR] [correlation-id] [path] - [error-message]
Example: [GATEWAY-ERROR] [550e8400-...] /api/v1/orders - Connection refused
```

---

## Development

### Project Structure

```
gateway-service/
├── src/
│   └── main/
│       ├── java/com/ecommerce/gateway/
│       │   ├── GatewayServiceApplication.java      # Main application
│       │   ├── config/
│       │   │   └── WebClientConfig.java            # WebClient bean
│       │   ├── filter/
│       │   │   ├── RequestLoggingFilter.java       # Request logging + correlation ID
│       │   │   └── ResponseLoggingFilter.java      # Response logging + duration
│       │   ├── exception/
│       │   │   └── GlobalErrorHandler.java         # RFC 7807 error handling
│       │   └── health/
│       │       ├── OrderServiceHealthIndicator.java
│       │       └── InventoryServiceHealthIndicator.java
│       └── resources/
│           ├── application.yml                      # Base configuration
│           └── application-dev.yml                  # Development configuration
└── pom.xml
```

### Adding New Routes

To add a new service route:

1. Update `application.yml`:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: new-service
          uri: http://localhost:8083
          predicates:
            - Path=/api/v1/new-resource/**
```

2. Create health indicator:
```java
@Component
public class NewServiceHealthIndicator implements HealthIndicator {
    // Similar to OrderServiceHealthIndicator
}
```

3. Update this README with new route documentation

---

## References

- [Spring Cloud Gateway Documentation](https://spring.io/projects/spring-cloud-gateway)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [RFC 7807 Problem Details](https://www.rfc-editor.org/rfc/rfc7807)
- [Project Architecture Rules](../AGENTS.md)
- [Gateway Integration Guidelines](../docs/gateway-integration-guidelines.md)
- [API Versioning Standard (ADR-002)](../DECISIONS.md)

---

## License

Part of the E-Commerce Microservices Platform educational project.

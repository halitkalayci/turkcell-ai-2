# Task 03: Gateway Observability and Health Checks

**Status:** Not Started  
**Priority:** Medium  
**Dependencies:** Task 02  
**Estimated Time:** 30 minutes

---

## Objective

Implement health checks and observability features for the gateway to monitor downstream services.

---

## Requirements

1. Configure Spring Boot Actuator endpoints
2. Implement custom health indicators for downstream services
3. Add Prometheus metrics (optional, for future monitoring)
4. Configure info endpoint with build information
5. Enable gateway metrics

---

## Checklist

### Actuator Configuration

- [ ] Enable health, info, metrics, gateway endpoints in `application.yml`
- [ ] Configure health endpoint to show details: `always`
- [ ] Enable liveness and readiness probes (for Kubernetes readiness)

### Custom Health Indicators

- [ ] Create `OrderServiceHealthIndicator.java`
  - Ping Order Service health endpoint
  - Return UP if service responds, DOWN otherwise
  
- [ ] Create `InventoryServiceHealthIndicator.java`
  - Ping Inventory Service health endpoint
  - Return UP if service responds, DOWN otherwise

### Gateway Metrics

- [ ] Enable gateway metrics in configuration
- [ ] Expose route-level metrics (request count, duration, errors)

### Info Endpoint

- [ ] Add application name, version, description to `application.yml`
- [ ] Add build timestamp
- [ ] Add Java version information

---

## Health Check Endpoints

Gateway exposes:
- `GET /actuator/health` - Overall health
- `GET /actuator/health/liveness` - Liveness probe
- `GET /actuator/health/readiness` - Readiness probe

Expected Response:
```json
{
  "status": "UP",
  "components": {
    "orderService": {
      "status": "UP",
      "details": {
        "uri": "http://localhost:8081/actuator/health"
      }
    },
    "inventoryService": {
      "status": "UP",
      "details": {
        "uri": "http://localhost:8082/actuator/health"
      }
    }
  }
}
```

---

## Metrics to Track

1. **Request Metrics**
   - Total requests per route
   - Average response time per route
   - Error rate per route

2. **Gateway Metrics**
   - Active connections
   - Pending requests
   - Timeout count

3. **Service Health**
   - Downstream service availability
   - Circuit breaker status (future enhancement)

---

## Testing Strategy

- [ ] `GET http://localhost:8080/actuator/health` returns 200 with all services UP
- [ ] Stop Order Service, health shows `orderService: DOWN`
- [ ] Stop Inventory Service, health shows `inventoryService: DOWN`
- [ ] Metrics endpoint shows route statistics
- [ ] Info endpoint shows correct application details

---

## Validation Criteria

- [ ] Health endpoint correctly reflects downstream service status
- [ ] Metrics are collected for all routes
- [ ] Info endpoint returns build information
- [ ] Gateway remains available even if downstream services are down

---

## Configuration Example

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,gateway
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  metrics:
    tags:
      application: ${spring.application.name}

info:
  app:
    name: Gateway Service
    description: API Gateway for E-Commerce Microservices
    version: 1.0.0
```

---

## References

- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Spring Cloud Gateway Metrics](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#actuator-api)

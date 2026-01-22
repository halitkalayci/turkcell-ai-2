# Task 01: Gateway Project Setup

**Status:** Not Started  
**Priority:** High  
**Dependencies:** None  
**Estimated Time:** 30 minutes

---

## Objective

Create a new Spring Cloud Gateway microservice project with proper configuration and dependencies.

---

## Requirements

1. Create `gateway-service` directory at project root
2. Initialize Maven project with Spring Boot 3.x parent
3. Add required dependencies:
   - `spring-cloud-starter-gateway`
   - `spring-boot-starter-actuator`
   - `spring-cloud-starter-config` (for future configuration management)
4. Create basic application structure following hexagonal architecture
5. Configure application properties for gateway

---

## File Structure to Create

```
gateway-service/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── ecommerce/
        │           └── gateway/
        │               ├── GatewayServiceApplication.java
        │               └── config/
        │                   └── GatewayConfig.java
        └── resources/
            ├── application.yml
            └── application-dev.yml
```

---

## Checklist

### 1. Maven Configuration (pom.xml)

- [ ] Set groupId: `com.ecommerce`
- [ ] Set artifactId: `gateway-service`
- [ ] Set version: `0.0.1-SNAPSHOT`
- [ ] Set Java version: `21`
- [ ] Add Spring Boot 3.x parent
- [ ] Add Spring Cloud BOM (2023.x.x for Spring Boot 3.x)
- [ ] Add dependency: `spring-cloud-starter-gateway`
- [ ] Add dependency: `spring-boot-starter-actuator`
- [ ] Add maven-compiler-plugin configuration

### 2. Main Application Class

- [ ] Create `GatewayServiceApplication.java`
- [ ] Add `@SpringBootApplication` annotation
- [ ] Add `@EnableDiscoveryClient` annotation (for future service discovery)
- [ ] Keep main method simple

### 3. Application Configuration (application.yml)

- [ ] Set server port: `8080` (Gateway will be the main entry point)
- [ ] Set application name: `gateway-service`
- [ ] Configure actuator endpoints
- [ ] Set logging level for gateway: `DEBUG` (for development)
- [ ] Add placeholder for routes configuration

### 4. Development Configuration (application-dev.yml)

- [ ] Enable verbose logging for Spring Cloud Gateway
- [ ] Configure CORS settings for development (allow all origins)
- [ ] Set timeout configurations

---

## Implementation Notes

### Port Assignment Strategy
- **Gateway:** `8080` (main entry point)
- **Order Service:** `8081`
- **Inventory Service:** `8082`

### Why These Choices?

1. **Spring Cloud Gateway**: Reactive, non-blocking gateway built on Spring WebFlux
2. **Port 8080**: Standard HTTP port, clients will only know this port
3. **Actuator**: Health checks and monitoring for gateway itself

---

## Validation Criteria

- [ ] Project builds successfully with `mvn clean install`
- [ ] Application starts without errors
- [ ] Actuator health endpoint accessible at `http://localhost:8080/actuator/health`
- [ ] No route configurations yet (will be added in next task)

---

## Contract-First Compliance

✅ **No OpenAPI contract needed** - Gateway is infrastructure, not a business service  
✅ **Architectural alignment** - Gateway is the edge/adapter layer in hexagonal architecture  

---

## References

- [Spring Cloud Gateway Documentation](https://spring.io/projects/spring-cloud-gateway)
- ADR-002: API Versioning Strategy
- AGENTS.md: Architecture rules

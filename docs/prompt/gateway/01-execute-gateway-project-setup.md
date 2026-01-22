# Prompt: Execute Gateway Project Setup (Task 01)

---

## ROLE

You are a **Senior Backend Java Engineer** specializing in Spring Boot microservices and **API Gateway patterns**. You have deep expertise in **Spring Cloud Gateway**, reactive programming with WebFlux, and implementing enterprise routing infrastructure following **contract-first** principles.

---

## CONTEXT

We are building a mini e-commerce microservices platform with:
- **Java 21** + **Spring Boot 3.x**
- **Contract-First** approach using OpenAPI/Swagger
- **Existing services:** Order Service (8081), Inventory Service (8082)
- **H2 in-memory database** for development

The platform currently has multiple microservices that clients access directly. We need to introduce an **API Gateway** as the **single entry point** to:
- Route all client requests to appropriate downstream services
- Inject correlation IDs for distributed tracing
- Log all requests/responses with timestamps
- Monitor downstream service health
- Handle connection errors with RFC 7807 Problem Details

The gateway will use **Spring Cloud Gateway** (reactive, WebFlux-based) and run on port **8080**.

**Key Architectural Rules (from AGENTS.md):**
- Gateway is the ONLY service clients access directly
- Gateway MUST NOT contain business logic
- Gateway routes requests based on path patterns
- All services follow `/api/v1` versioning (ADR-002)
- Services remain independently runnable on their own ports

**Reference Documents:**
- `.ai/tasks/gateway/01-gateway-project-setup.md` (task definition)
- `docs/gateway-integration-guidelines.md` (integration rules)
- `AGENTS.md` (architecture standards)
- `DECISIONS.md` (ADR-002: API Versioning)

**Existing Services:**
- `order-service/` - Reference for project structure
- `inventory-service/` - Reference for Spring Boot configuration

---

## TASK

Implement **Task 01: Gateway Project Setup** by creating a new Spring Cloud Gateway microservice with proper configuration and dependencies.

### 1. Project Structure Creation
Create the `gateway-service/` directory at project root with:
- `pom.xml` with Spring Boot 3.x parent and Spring Cloud dependencies
- Standard Spring Boot application structure under `src/main/java/com/ecommerce/gateway/`
- Configuration files in `src/main/resources/`

### 2. Maven Configuration (pom.xml)
- Set groupId: `com.ecommerce`
- Set artifactId: `gateway-service`
- Set version: `0.0.1-SNAPSHOT`
- Set Java version: `21`
- Add Spring Boot 3.x parent (match order-service version)
- Add Spring Cloud BOM (compatible version for Spring Boot 3.x)
- Add dependency: `spring-cloud-starter-gateway` (reactive gateway)
- Add dependency: `spring-boot-starter-actuator` (health checks)
- Add maven-compiler-plugin configuration (Java 21)
- **DO NOT** add `spring-boot-starter-web` (conflicts with WebFlux)

### 3. Main Application Class
Create `com.ecommerce.gateway.GatewayServiceApplication`:
- Add `@SpringBootApplication` annotation
- Simple main method with `SpringApplication.run()`
- Keep it minimal - no additional configuration here

### 4. Application Configuration (application.yml)
Configure:
- Server port: `8080` (main entry point for clients)
- Application name: `gateway-service`
- Actuator endpoints: Enable `health`, `info`, `gateway`
- Actuator health details: `show-details: always`
- Logging level for gateway: `DEBUG` (for development)
- **Placeholder** for routes configuration (empty for now, will be added in Task 02)

### 5. Development Configuration (application-dev.yml)
Configure:
- Verbose logging for Spring Cloud Gateway: `DEBUG`
- CORS settings: Allow all origins for development (pattern: `'*'`)
- Allowed methods: GET, POST, PUT, DELETE
- Allowed headers: `'*'`
- Gateway timeouts: 30 seconds for requests

### 6. Basic Configuration Class (optional)
Create `com.ecommerce.gateway.config.GatewayConfig` if needed:
- Empty for now (routes will be added in Task 02)
- Add comments indicating this is where custom filters will be defined

---

## CONSTRAINTS

**MUST NOT:**
- Add any route configurations yet (Task 02 scope)
- Add any custom filters or handlers yet (Task 02 scope)
- Implement security, authentication, or authorization (future scope)
- Add service discovery dependencies (Eureka/Consul) - not implemented yet
- Use OpenAPI generator tools (manual implementation only)
- Modify any existing service files (order-service, inventory-service)
- Create files outside of `gateway-service/` directory
- Add `spring-boot-starter-web` dependency (conflicts with reactive gateway)
- Implement any business logic in gateway

**MUST:**
- Use Spring Cloud Gateway (reactive, WebFlux-based)
- Use port 8080 for gateway (main entry point)
- Enable Spring Boot Actuator for health checks
- Follow same package structure as order-service and inventory-service
- Keep application class simple and minimal
- Add proper Maven Spring Cloud BOM for dependency management
- Configure logging for gateway requests (even if no routes yet)
- Include CORS configuration for development mode

**VERIFY:**
- Project builds successfully with `mvn clean install`
- Application starts without errors
- Actuator health endpoint accessible at `http://localhost:8080/actuator/health`
- No port conflicts with existing services (8081, 8082)
- Swagger UI not needed for gateway (infrastructure service)

---

## OUTPUT

### Step 1: File Breakdown Plan
Before writing any code, provide a **FILE BREAKDOWN** listing:

```
gateway-service/
├── pom.xml (dependencies: Spring Cloud Gateway, Actuator)
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── ecommerce/
│       │           └── gateway/
│       │               ├── GatewayServiceApplication.java (main class)
│       │               └── config/
│       │                   └── GatewayConfig.java (placeholder for routes)
│       └── resources/
│           ├── application.yml (base config, port 8080)
│           └── application-dev.yml (dev config, CORS, logging)
```

**Purpose of each file:**
- `pom.xml` - Maven config with Spring Cloud Gateway and Actuator
- `GatewayServiceApplication.java` - Spring Boot entry point
- `GatewayConfig.java` - Configuration placeholder (routes in Task 02)
- `application.yml` - Base configuration for all environments
- `application-dev.yml` - Development-specific settings

Wait for confirmation before proceeding.

### Step 2: Implementation in Single Batch
Since Task 01 is foundational setup, implement all files together:

**All files:**
1. `pom.xml` with proper dependencies
2. `GatewayServiceApplication.java`
3. `application.yml`
4. `application-dev.yml`
5. `GatewayConfig.java` (optional placeholder)

### Step 3: Build Verification
After implementation, execute these commands and report results:

```bash
cd gateway-service
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Step 4: Verification Checklist
Provide verification results:

- [ ] `mvn clean install` succeeds without errors
- [ ] Application starts without exceptions
- [ ] Logs show: "Started GatewayServiceApplication"
- [ ] Actuator health check responds: `GET http://localhost:8080/actuator/health`
- [ ] Health response shows: `{"status":"UP"}`
- [ ] No route configuration present yet (expected for Task 01)
- [ ] No port conflicts with order-service (8081) or inventory-service (8082)

### Step 5: Testing Evidence
Provide curl test results:

```bash
# Test 1: Health Check
curl http://localhost:8080/actuator/health

# Expected Response:
{
  "status": "UP"
}

# Test 2: Gateway Actuator Endpoint
curl http://localhost:8080/actuator/gateway/routes

# Expected Response:
[] (empty array - no routes configured yet)
```

---

## EXAMPLES

### Example: Maven Dependency Configuration

```xml
<dependencies>
    <!-- Spring Cloud Gateway (Reactive) -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>
    
    <!-- Actuator for Health Checks -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <!-- Spring Cloud BOM -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.0</version> <!-- Compatible with Spring Boot 3.x -->
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Example: Application Configuration

```yaml
# application.yml
server:
  port: 8080

spring:
  application:
    name: gateway-service
  cloud:
    gateway:
      routes: [] # Empty for now, will be configured in Task 02

management:
  endpoints:
    web:
      exposure:
        include: health,info,gateway
  endpoint:
    health:
      show-details: always

logging:
  level:
    org.springframework.cloud.gateway: DEBUG
```

### Example: Development Configuration

```yaml
# application-dev.yml
spring:
  cloud:
    gateway:
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
      httpclient:
        connect-timeout: 5000
        response-timeout: 30s

logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    reactor.netty: DEBUG
```

### Example: Testing After Setup

```bash
# Start gateway
cd gateway-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# In another terminal - test health
curl http://localhost:8080/actuator/health

# Should return:
{
  "status": "UP"
}

# Test gateway routes endpoint (should be empty)
curl http://localhost:8080/actuator/gateway/routes

# Should return:
[]
```

### Example: Verify No Route Configuration Yet

```bash
# Try accessing order service through gateway (should fail - no routes yet)
curl http://localhost:8080/api/v1/orders

# Expected: 404 Not Found (no route configured)
# This is correct behavior for Task 01!
```

---

## SUCCESS CRITERIA

Task 01 is complete when:

✅ Gateway project structure created with proper package layout  
✅ Maven build succeeds (`mvn clean install`)  
✅ Application starts without errors on port 8080  
✅ Actuator health endpoint accessible and returns UP  
✅ No route configurations present (intentional - Task 02 scope)  
✅ No port conflicts with existing services  
✅ Logs show Spring Cloud Gateway initialization  
✅ CORS configured for development mode  
✅ Ready for Task 02 (route configuration)

---

## NEXT STEPS

After Task 01 completion:
- **Task 02:** Configure routes for Order Service and Inventory Service
- **Task 03:** Implement observability and health checks for downstream services
- **Task 04:** Create documentation and testing guidelines

**DO NOT PROCEED** to Task 02 until Task 01 is fully verified and approved.

---

## REFERENCES

- `.ai/tasks/gateway/01-gateway-project-setup.md` - Full task definition
- `docs/gateway-integration-guidelines.md` - Gateway rules and patterns
- `AGENTS.md` - Architecture standards (Section 5: Gateway Rules)
- `DECISIONS.md` - ADR-002 (API Versioning), ADR-003 (Gateway Pattern)
- [Spring Cloud Gateway Docs](https://spring.io/projects/spring-cloud-gateway)
- [Spring Boot Actuator Docs](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

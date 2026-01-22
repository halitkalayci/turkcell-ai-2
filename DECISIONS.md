# ARCHITECTURE DECISION RECORDS - E-COMMERCE PLATFORM
Last Updated: 2026-01-21


This document records the architectural decisions made for this project.

**AI AGENTS & DEVELOPERS:** You MUST adhere to these decisions. DO NOT refactor.

---

## ADR-001: Database Selection
Last Updated: 2026-01-21

* **Status:** Accepted
* **Decision:** H2 In-Memory
* **Reason:** We will only use this project for education purposes.
* **Alternatives Considered:** PostgreSQL, MySQL

---

## ADR-002: API Versioning Strategy
Last Updated: 2026-01-21

* **Status:** Accepted
* **Decision:** Path-based API versioning with `/api/v1` prefix for all service endpoints
* **Reason:** 
  - Compliance with AGENTS.md Line 66-72 versioning mandate
  - Ensures backward compatibility for future API evolution
  - Prevents silent breaking changes to clients
  - Establishes clear contract-first baseline (v1) for all services
* **Implementation:**
  - All REST endpoints prefixed with `/api/v1` (e.g., `/api/v1/orders`, `/api/v1/orders/{id}`)
  - OpenAPI contract updated to reflect versioned paths
  - SpringDoc api-docs path: `/api/v1/api-docs`
  - Version number in path, not headers or query parameters
* **Migration Strategy:**
  - v1 is the baseline version (no existing clients to migrate)
  - Future v2 will coexist with v1 for transition period
  - Breaking changes require new version; v1 remains stable
  - Deprecation notices will precede removal of old versions
* **Alternatives Considered:** 
  - Header-based versioning (rejected: less visible, harder to test)
  - Query parameter versioning (rejected: not RESTful)
  - No versioning (rejected: violates AGENTS.md standards)

---

## ADR-003: API Gateway Pattern
Last Updated: 2026-01-22

* **Status:** Accepted
* **Decision:** Spring Cloud Gateway as single entry point for all microservices
* **Reason:** 
  - Centralized routing and request handling
  - Single entry point simplifies client configuration
  - Enables future cross-cutting concerns (auth, rate limiting, monitoring)
  - Correlation ID injection for distributed tracing
  - Health monitoring of all downstream services
* **Implementation:**
  - Gateway runs on port 8080 (main entry point)
  - All microservices accessed via gateway (clients never call services directly in production)
  - Path-based routing: `/api/v1/{resource}` → service
  - No business logic in gateway (pure routing/logging/monitoring)
  - Services remain independently runnable on their own ports (8081+)
* **Technology Choice:**
  - Spring Cloud Gateway (reactive, non-blocking)
  - WebFlux-based (async I/O)
  - Actuator for health checks
* **Migration Strategy:**
  - Services can still be accessed directly during development
  - Production clients will only know gateway URL
  - Gradual migration: direct → gateway over time
* **Alternatives Considered:** 
  - Nginx (rejected: less Spring ecosystem integration)
  - Spring Cloud Netflix Zuul (rejected: maintenance mode)
  - Kong (rejected: adds external dependency, overkill for our scale)
  - No gateway (rejected: clients would need to know all service locations)

---

## ADR-004: Gateway Routing Strategy
Last Updated: 2026-01-22

* **Status:** Accepted
* **Decision:** Path-based routing with resource-specific route definitions
* **Reason:** 
  - Clear and predictable routing rules
  - Aligns with RESTful resource-based API design
  - Easy to understand and maintain
  - Enables per-resource filtering and monitoring
* **Implementation:**
  - One route per top-level resource (e.g., `/orders`, `/inventory-items`, `/reservations`)
  - No prefix stripping - preserve `/api/v1` in forwarded requests
  - Correlation ID injection on all routes
  - Request/response logging on all routes
* **Example:**
  ```yaml
  /api/v1/orders/** → Order Service (8081)
  /api/v1/inventory-items/** → Inventory Service (8082)
  /api/v1/reservations/** → Inventory Service (8082)
* **Alternatives Considered:** 
  - Service-based routing (e.g., /order-service/**) - rejected: exposes internal service names
  - Header-based routing - rejected: not RESTful, harder to test
  - Wildcard catch-all route - rejected: too broad, loses routing clarity
# ARCHITECTURE DECISION RECORDS - E-COMMERCE PLATFORM
Last Updated: 2026-01-22


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

---

## ADR-005: Spring Cloud Stream for Messaging Abstraction
Last Updated: 2026-01-22

* **Status:** Accepted
* **Decision:** Spring Cloud Stream as messaging abstraction layer
* **Reason:** 
  - Decouples application code from specific message broker
  - Enables switching between Kafka, RabbitMQ, or other brokers via configuration
  - Provides unified programming model (functional style)
  - Built-in retry, DLQ, and error handling
  - Future-proof architecture for technology changes
* **Implementation:**
  - Spring Cloud Stream with functional programming model
  - Current binder: Kafka (localhost:29023)
  - Define bindings as Supplier (producer), Function (processor), Consumer (consumer)
  - No direct Kafka API usage in application code
  - Binder configuration in application.yml
* **Example:**
  ```java
  @Bean
  public Supplier<Message<OrderCreatedEvent>> orderCreatedPublisher() {
      // Producer implementation
  }
  
  @Bean
  public Consumer<Message<OrderCreatedEvent>> orderCreatedConsumer() {
      // Consumer implementation
  }
  ```
* **Migration Strategy:**
  - Start with Kafka binder for development
  - Can switch to RabbitMQ by changing dependency + configuration
  - No application code changes needed for binder switch
* **Alternatives Considered:** 
  - Direct Spring Kafka usage (rejected: tight coupling to Kafka)
  - JMS API (rejected: less modern, limited features)
  - Custom abstraction layer (rejected: reinventing the wheel)

---

## ADR-006: Transactional Outbox Pattern
Last Updated: 2026-01-22

* **Status:** Accepted
* **Decision:** Mandatory Transactional Outbox Pattern for all message producers
* **Reason:** 
  - Guarantees atomic write to DB + event publishing
  - Prevents data inconsistency (DB committed but message broker failed)
  - Ensures "at-least-once" delivery guarantee
  - Survives message broker downtime
  - Works seamlessly with Spring Cloud Stream
* **Implementation:**
  - Each service has `outbox_events` table in its database
  - Business transaction writes to domain tables + outbox in same TX
  - Separate @Scheduled publisher polls outbox every 10 seconds
  - Publisher reads unpublished events, sends via Spring Cloud Stream, marks as published
  - No cleanup (events remain for audit trail)
* **Outbox Table Schema:**
  ```sql
  CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP,
    status VARCHAR(20) NOT NULL  -- PENDING, PUBLISHED, FAILED
  );
  ```
* **Spring Cloud Stream Integration:**
  - Outbox publisher uses StreamBridge to send messages
  - Dynamic destination routing via StreamBridge
* **Alternatives Considered:** 
  - Direct message publishing (rejected: no transactional guarantee)
  - Change Data Capture/Debezium (rejected: added complexity)
  - Two-phase commit (rejected: performance overhead)

---

## ADR-007: Consumer Reliability Patterns
Last Updated: 2026-01-22

* **Status:** Accepted
* **Decision:** Idempotency + Retry + DLQ for all message consumers
* **Reason:** 
  - Message brokers typically guarantee "at-least-once" delivery (duplicates possible)
  - Network failures require retry mechanisms
  - Poison messages must not block processing
  - Consumer crashes must not lose messages
* **Implementation:**
  - **Idempotency:** 
    - Each service has `processed_events` table
    - Columns: event_id (PK), processed_at, event_type
    - Before processing, check if event_id exists
    - If exists, skip processing (already handled)
    - Insert event_id after successful processing (in same TX as business logic)
  - **Retry:**
    - Spring Cloud Stream retry configuration
    - Max 3 retries with delays: 5s, 15s, 45s (exponential backoff)
    - After max retries → send to DLQ
  - **Dead Letter Queue:**
    - DLQ destination per service: `order-service.dlq`, `inventory-service.dlq`, `notification-service.dlq`
    - Store failed message with error metadata
    - Manual review/reprocessing workflow (future enhancement)
* **Processed Events Table Schema:**
  ```sql
  CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL
  );
  ```
* **Spring Cloud Stream Configuration:**
  ```yaml
  spring:
    cloud:
      stream:
        bindings:
          {binding-name}:
            consumer:
              max-attempts: 3
              back-off-initial-interval: 5000
              back-off-multiplier: 3.0
        kafka:
          bindings:
            {binding-name}:
              consumer:
                enable-dlq: true
                dlq-name: {service-name}.dlq
  ```
* **Alternatives Considered:** 
  - No idempotency (rejected: duplicate processing risk)
  - Infinite retry (rejected: blocks partition processing)
  - No DLQ (rejected: silent data loss)

---

## ADR-008: JSON Event Serialization
Last Updated: 2026-01-22

* **Status:** Accepted
* **Decision:** JSON for event serialization
* **Reason:** 
  - Human-readable (easier debugging)
  - No schema registry infrastructure needed
  - Sufficient for project scale (3 services)
  - Spring Cloud Stream native JSON support
  - Standard Jackson serialization
  - Works with any binder (Kafka, RabbitMQ, etc.)
* **Implementation:**
  - Spring Cloud Stream JSON message converter
  - All events follow standard envelope structure
  - Money fields serialized as strings (BigDecimal → String)
  - Timestamps in ISO-8601 format
* **Event Envelope Standard:**
  ```json
  {
    "eventId": "uuid",
    "eventType": "string",
    "eventTimestamp": "ISO-8601",
    "correlationId": "uuid",
    "aggregateId": "uuid",
    "payload": { }
  }
  ```
* **Spring Cloud Stream Configuration:**
  ```yaml
  spring:
    cloud:
      stream:
        bindings:
          {binding-name}:
            content-type: application/json
  ```
* **No Versioning:**
  - Events are immutable
  - Breaking changes = new event type (e.g., OrderCreatedV2)
  - Backward compatible changes allowed in payload
* **Alternatives Considered:** 
  - Avro with Schema Registry (rejected: overkill for project size)
  - Protobuf (rejected: added complexity, less readable)

---

## ADR-009: Keycloak with Operation-Based Claims Security Architecture
Last Updated: 2026-01-22

* **Status:** ACCEPTED
* **Decision:** Keycloak 26.5.1 for centralized IAM with operation-based claims authorization
* **Reason:** 
  - Centralized identity management (Single Source of Truth)
  - Fine-grained permission control (operation-based claims, not roles)
  - OAuth2/OIDC industry standards
  - Token-based stateless security (scalable)
  - SSO support for future requirements
  - Open-source, mature, production-ready
* **Technology Stack:**
  - **Identity Provider**: Keycloak 26.5.1
  - **Protocol**: OAuth2 / OpenID Connect
  - **Token Format**: JWT (Bearer tokens)
  - **Realm**: `ecommerce`
* **Authorization Model:**
  - **Operation-Based Claims** (NOT role-based)
  - Format: `<resource>.<operation>` (e.g., `order.create`, `inventory.read`)
  - Claims implemented as Keycloak client roles
  - Composite roles group common claims (Customer, Admin, InventoryManager, etc.)
* **Architecture:**
  ```
  Client → Gateway (OAuth2 Client) → Keycloak (Auth) → JWT Token
           ↓ (TokenRelay)
        Backend Services (Resource Server) → Validate JWT → Extract Claims → Authorize
  ```
* **Implementation:**
  - Gateway: OAuth2 Client + Resource Server (validates & forwards tokens)
  - Backend Services: Resource Server (validates tokens, extracts claims)
  - All endpoints protected with `@PreAuthorize("hasAuthority('claim')")`
  - User context extracted from JWT (`sub`, `preferred_username`, custom claims)
* **Token Lifespan:**
  - Access Token: 15 minutes
  - Refresh Token: 30 minutes
  - SSO Session: 10 hours
* **Security Features:**
  - Cryptographic signatures (RS256)
  - Token expiration and refresh
  - Defense in depth (gateway + backend validation)
  - Stateless authentication (no server-side sessions)
* **Consequences:**
  - **Positive**: Centralized user management, fine-grained authorization, scalability, SSO-ready
  - **Negative**: Initial setup complexity, token size increase, operational overhead (HA required)
* **Alternatives Considered:**
  - Spring Security + Database (rejected: no SSO, code duplication)
  - Auth0/Okta (rejected: vendor lock-in, cost)
  - Custom OAuth2 Server (rejected: reinventing the wheel)
* **Documentation:**
  - Full ADR: [docs/architecture/security-architecture.md](docs/architecture/security-architecture.md)
  - Setup Guide: [docs/security/keycloak-integration.md](docs/security/keycloak-integration.md)
  - Claims Reference: [docs/security/operation-claims-guide.md](docs/security/operation-claims-guide.md)
  - Token Structure: [docs/security/jwt-token-structure.md](docs/security/jwt-token-structure.md)
  - Testing Guide: [docs/security/security-testing-guide.md](docs/security/security-testing-guide.md)
  - AGENTS.md Section 7: Security & Authentication rules
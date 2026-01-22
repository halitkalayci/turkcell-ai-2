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

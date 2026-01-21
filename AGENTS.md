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

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

If any detail is missing, ASK.

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


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
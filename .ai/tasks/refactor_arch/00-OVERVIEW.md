# Hexagonal Architecture Refactoring - Project Overview

**Project:** E-Commerce Microservices Architecture Refactoring  
**Date:** 2026-01-21  
**Status:** Planning Phase  
**Estimated Duration:** 5-7 days  

---

## Executive Summary

AGENTS.md'de tanımlanan **Hexagonal Architecture** kurallarına göre `order-service` ve `inventory-service` servislerinin tamamının yeniden yapılandırılması.

---

## Current State Analysis

### Mevcut Mimari Sorunlar

Her iki serviste de şu sorunlar tespit edildi:

1. **Katman Karmaşası**
   - Service katmanı hem business logic hem de infrastructure concerns içeriyor
   - Entity'ler hem domain model hem de persistence model olarak kullanılıyor
   - Controller'lar doğrudan JPA entity'leriyle değil DTO'larla çalışıyor (iyi) ama service katmanı karışık

2. **Hexagonal Architecture İhlali**
   - Application layer yok (use-case orchestration eksik)
   - Domain layer yok (business rules entity'lerde dağınık)
   - Port/Adapter pattern uygulanmamış
   - Dependency direction yanlış (domain infrastructure'a bağımlı)

3. **Business Logic Dağılımı**
   - `InventoryItem` entity'sinde business logic var (`isAvailable()`, `reserve()`)
   - `Order` entity'sinde business logic olması gerekiyor ama eksik
   - ServiceImpl'de hem orchestration hem business logic hem persistence

4. **Test Edilebilirlik Sorunu**
   - Business logic infrastructure'a sıkı bağımlı
   - Unit test yazmak zor (JPA, transaction gerekli)

---

## Target Architecture (AGENTS.md Compliance)

```
┌──────────────────────────────────────────────────────────────┐
│                    WEB/API LAYER (controller)                 │
│  - No business logic                                          │
│  - Validates input, maps DTOs                                 │
│  - Returns DTOs (NEVER entities)                              │
└────────────────────┬─────────────────────────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────────────────────────┐
│              APPLICATION LAYER (use-cases)                    │
│  - Orchestrates use-cases                                     │
│  - Transaction management                                     │
│  - Calls domain services via ports                            │
│  - MUST BE unit-testable (no framework dependency)            │
└────────────────────┬─────────────────────────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────────────────────────┐
│              DOMAIN LAYER (business rules)                    │
│  - Domain Model (entities/value objects)                      │
│  - Domain Services                                            │
│  - Business invariants                                        │
│  - NO framework annotations                                   │
│  - Port interfaces (outbound)                                 │
└────────────────────┬─────────────────────────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────────────────────────┐
│           INFRASTRUCTURE LAYER (adapters)                     │
│  - JPA entities (persistence model)                           │
│  - Repositories (implement ports)                             │
│  - External clients (REST, messaging)                         │
│  - Mappers (domain ↔ persistence)                             │
└──────────────────────────────────────────────────────────────┘
```

---

## Key Principles to Enforce

### 1. Dependency Rule (MANDATORY)
- Dependencies ALWAYS point inward
- Domain depends on NOTHING
- Application depends ONLY on Domain
- Infrastructure depends on Application + Domain
- Web depends on Application

### 2. Separation of Concerns
- **Domain Model** ≠ **Persistence Model**
- Domain entities: pure business logic, no JPA annotations
- JPA entities: only persistence concerns
- Mapper pattern between them

### 3. Port & Adapter Pattern
- **Ports (interfaces):** defined in domain/application
- **Adapters (implementations):** in infrastructure
- Examples:
  - Port: `InventoryRepository` (domain interface)
  - Adapter: `JpaInventoryRepositoryAdapter` (infrastructure implementation)

### 4. Business Logic Location
- Business rules ONLY in Domain layer
- Application layer orchestrates, does NOT contain business logic
- Entity behavior methods in domain entities

---

## Scope of Work

### Services to Refactor
1. **inventory-service** (Primary)
2. **order-service** (Secondary)

### Out of Scope
- notification-service (not yet implemented)
- Database schema changes
- API contract changes (OpenAPI remains same)
- Adding new features

---

## Success Criteria

- [✓] All AGENTS.md rules satisfied
- [✓] Hexagonal architecture properly implemented
- [✓] Domain layer has NO framework dependencies
- [✓] Application layer is unit-testable without Spring context
- [✓] All existing API tests pass
- [✓] No behavioral changes (same contracts)
- [✓] Code quality improved (separation of concerns)

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Breaking existing behavior | HIGH | Extensive testing, no API changes |
| Over-engineering | MEDIUM | Follow AGENTS.md simplicity principle |
| Incomplete refactoring | MEDIUM | Strict task decomposition and review |
| Team confusion | LOW | Clear documentation in each task |

---

## Task Decomposition Structure

```
.ai/tasks/refactor_arch/
├── 00-OVERVIEW.md              (this file)
├── 01-INVENTORY-ANALYSIS.md    (current state analysis)
├── 02-INVENTORY-DOMAIN.md      (domain layer creation)
├── 03-INVENTORY-APPLICATION.md (application layer creation)
├── 04-INVENTORY-INFRASTRUCTURE.md (infrastructure refactoring)
├── 05-INVENTORY-WEB.md         (controller updates)
├── 06-ORDER-ANALYSIS.md        (order service analysis)
├── 07-ORDER-DOMAIN.md          (order domain layer)
├── 08-ORDER-APPLICATION.md     (order application layer)
├── 09-ORDER-INFRASTRUCTURE.md  (order infrastructure)
├── 10-ORDER-WEB.md             (order controller updates)
└── 11-VALIDATION.md            (final validation & testing)
```

---

## AI Agent Execution Guidelines

1. **AUTONOMOUS OPERATION** - No human intervention required
2. Each task file provides:
   - Current state analysis requirements
   - Target state specifications
   - Detailed file breakdown with line estimates
   - Concrete implementation patterns
   - Automated validation criteria
3. Tasks MUST be executed IN ORDER (dependencies)
4. Each task completion requires automated validation before proceeding
5. Follow AGENTS.md rule: Analyze codebase thoroughly, then generate code
6. **Decision Framework:**
   - If specification unclear: Analyze existing code patterns
   - If choice between approaches: Select simplest that satisfies AGENTS.md
   - If data missing: Extract from codebase (never invent)
   - If validation fails: Rollback and re-analyze

## AI Agent Operation Mode

- **Pattern Recognition:** Learn from inventory-service, apply to order-service
- **Strict Compliance:** AGENTS.md rules are absolute
- **No Invention:** All decisions derived from existing code + rules
- **Continuous Validation:** Test after each file generation
- **Self-Correcting:** If validation fails, analyze and retry

---

## Next Steps (AI Agent Autonomous Execution)

1. **Load Task 01:** Parse INVENTORY-ANALYSIS task specifications
2. **Analyze Codebase:** Scan inventory-service current structure
3. **Identify Violations:** Map violations against AGENTS.md rules
4. **Generate Analysis Report:** Document findings in structured format
5. **Proceed to Task 02:** Begin domain layer generation

**NO HUMAN APPROVAL REQUIRED** - Agent proceeds autonomously through task sequence.

---

**AI Agent Execution Mode:** Autonomous refactoring with continuous validation.

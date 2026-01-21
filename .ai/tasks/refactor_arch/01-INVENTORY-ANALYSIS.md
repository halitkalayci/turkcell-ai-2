# Task 01: Inventory Service - Current State Analysis

**Task ID:** REFACTOR-01  
**Service:** inventory-service  
**Phase:** Analysis  
**Estimated Time:** 2 hours  
**Dependencies:** None  

---

## Objective

Mevcut `inventory-service` kodunun detaylı analizi ve AGENTS.md kurallarına göre sapmaların belirlenmesi.

---

## Current Package Structure

```
com.ecommerce.inventoryservice/
├── InventoryServiceApplication.java
├── config/
│   └── OpenApiConfig.java
├── controller/
│   └── InventoryController.java
├── dto/
│   ├── request/
│   │   ├── AvailabilityCheckRequest.java
│   │   └── StockReservationRequest.java
│   └── response/
│       ├── AvailabilityCheckResponse.java
│       ├── InventoryItemResponse.java
│       └── StockReservationResponse.java
├── entity/
│   ├── InventoryItem.java
│   ├── Reservation.java
│   ├── ReservationItem.java
│   └── ReservationStatus.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── InsufficientStockException.java
│   ├── ProductNotFoundException.java
│   └── ReservationConflictException.java
├── repository/
│   ├── InventoryItemRepository.java
│   └── ReservationRepository.java
└── service/
    ├── InventoryService.java (interface)
    └── InventoryServiceImpl.java
```

---

## Files to Analyze

### 1. Controller Layer
- [x] `controller/InventoryController.java`
  - **Current State:** Clean, follows DTO pattern, no business logic
  - **AGENTS.md Compliance:** ✅ GOOD
  - **Issues:** None
  - **Action Required:** Minor adjustments for application layer integration

### 2. Service Layer
- [x] `service/InventoryService.java` (interface)
  - **Current State:** Interface with 6 methods
  - **Issues:** Mixing use-case orchestration with business operations
  - **Action Required:** Split into:
    - Application layer (use-cases)
    - Domain services (business logic)

- [x] `service/InventoryServiceImpl.java`
  - **Current State:** Implementation with business logic + persistence
  - **Issues:**
    - Contains business rules (should be in domain)
    - Direct repository usage (should use ports)
    - Transaction management mixed with logic
    - NOT unit-testable without Spring
  - **Action Required:** Complete refactoring

### 3. Entity Layer
- [x] `entity/InventoryItem.java`
  - **Issues:**
    - JPA annotations mixed with business logic
    - Methods like `isAvailable()`, `reserve()` are domain logic
    - Uses `LocalDateTime` instead of `Instant` (AGENTS.md violation - line 70)
  - **Action Required:** 
    - Create domain model `com.ecommerce.inventoryservice.domain.model.InventoryItem`
    - Keep JPA entity as persistence model
    - Create mapper

- [x] `entity/Reservation.java`
  - **Issues:** Same as InventoryItem
  - **Action Required:** Create domain model + mapper

- [x] `entity/ReservationStatus.java` (enum)
  - **Current State:** Clean enum
  - **Action Required:** Move to domain layer (no JPA dependency)

### 4. Repository Layer
- [x] `repository/InventoryItemRepository.java`
  - **Current State:** Spring Data JPA repository
  - **Issues:** Domain depends on Spring Data interfaces
  - **Action Required:**
    - Define port interface in domain
    - Create adapter in infrastructure

### 5. Exception Layer
- [x] All exception classes
  - **Current State:** Domain exceptions (good)
  - **Issues:** Located at root, should be in domain layer
  - **Action Required:** Move to `domain.exception`

### 6. DTO Layer
- [x] All DTOs (request/response)
  - **Current State:** Clean separation
  - **Action Required:** Keep in `controller.dto` or move to `web.dto`

---

## Identified Violations of AGENTS.md

### Critical Issues

1. **No Hexagonal Architecture** (AGENTS.md Section 3)
   - Missing application layer ❌
   - Missing domain layer ❌
   - No port/adapter separation ❌

2. **Entity = Domain Model = Persistence Model** (AGENTS.md Line 90-98)
   - JPA annotations in business logic ❌
   - Framework coupling in domain ❌

3. **Time Type Violation** (AGENTS.md Line 151-153)
   - Uses `LocalDateTime` instead of `Instant` ❌
   - Found in: `InventoryItem.lastUpdatedAt`, `Reservation.createdAt/expiresAt`

4. **Service Layer Confusion** (AGENTS.md Section 3.2-3.4)
   - Business logic in service implementation ❌
   - No clear use-case orchestration ❌

### Medium Issues

5. **Dependency Direction** (Hexagonal principle)
   - Domain depends on infrastructure (Spring Data) ❌

6. **Unit Testing Difficulty**
   - Cannot test business logic without Spring context ❌
   - Repository mocking required for business rule testing ❌

---

## Business Rules to Extract

From `InventoryItem.java`:
- `isAvailable(int quantity)` - availability check logic
- `reserve(int quantity)` - reservation logic
- `releaseReservation(int quantity)` - release logic

From `Reservation.java`:
- `isExpired()` - expiration check
- Status transitions (PENDING → CONFIRMED → EXPIRED)

From `InventoryServiceImpl.java`:
- Stock availability validation
- Reservation creation logic
- Reservation confirmation/cancellation logic
- Concurrent modification handling

---

## Target Package Structure

```
com.ecommerce.inventoryservice/
├── InventoryServiceApplication.java
├── web/                           [NEW]
│   ├── controller/
│   │   └── InventoryController.java
│   └── dto/
│       ├── request/
│       └── response/
├── application/                   [NEW]
│   ├── usecase/
│   │   ├── GetInventoryItemUseCase.java
│   │   ├── CheckAvailabilityUseCase.java
│   │   ├── CreateReservationUseCase.java
│   │   ├── GetReservationUseCase.java
│   │   ├── ConfirmReservationUseCase.java
│   │   └── CancelReservationUseCase.java
│   └── port/
│       ├── in/                    [Use case interfaces]
│       └── out/                   [Repository ports]
├── domain/                        [NEW]
│   ├── model/
│   │   ├── InventoryItem.java    [Pure domain - no JPA]
│   │   ├── Reservation.java      [Pure domain - no JPA]
│   │   ├── ReservationItem.java  [Pure domain - no JPA]
│   │   └── ReservationStatus.java
│   ├── service/
│   │   ├── StockService.java
│   │   └── ReservationService.java
│   ├── exception/
│   │   ├── InsufficientStockException.java
│   │   ├── ProductNotFoundException.java
│   │   └── ReservationConflictException.java
│   └── port/                      [Repository interfaces - outbound]
│       ├── InventoryRepository.java
│       └── ReservationRepository.java
├── infrastructure/
│   ├── persistence/
│   │   ├── entity/
│   │   │   ├── InventoryItemEntity.java  [JPA entity]
│   │   │   ├── ReservationEntity.java    [JPA entity]
│   │   │   └── ReservationItemEntity.java
│   │   ├── repository/
│   │   │   ├── InventoryItemJpaRepository.java  [Spring Data]
│   │   │   └── ReservationJpaRepository.java    [Spring Data]
│   │   ├── adapter/
│   │   │   ├── InventoryRepositoryAdapter.java  [Port implementation]
│   │   │   └── ReservationRepositoryAdapter.java
│   │   └── mapper/
│   │       ├── InventoryItemMapper.java
│   │       └── ReservationMapper.java
│   └── config/
│       └── OpenApiConfig.java
└── config/                        [Application config]
    └── UseCaseConfig.java         [Bean definitions]
```

---

## Key Decisions

### 1. Time Type Migration
- **From:** `LocalDateTime`
- **To:** `Instant`
- **Reason:** AGENTS.md Line 151-153, timezone-safe, UTC-based
- **Impact:** 
  - `InventoryItem.lastUpdatedAt`
  - `Reservation.createdAt`
  - `Reservation.expiresAt`

### 2. ID Strategy
- **Current:** UUID (correct ✅)
- **Action:** Keep UUID everywhere per AGENTS.md Line 158

### 3. Domain Model Independence
- **Decision:** Domain entities will have ZERO Spring/JPA dependencies
- **Approach:** Separate persistence entities + mappers

### 4. Port Naming Convention
- **Input Ports:** `*UseCase` interfaces in `application.port.in`
- **Output Ports:** `*Repository` interfaces in `domain.port`
- **Adapters:** `*Adapter` classes in `infrastructure.persistence.adapter`

---

## AI Agent Decision Rules (AUTO-RESOLVE)

### 1. Money Type in Inventory
**Analysis Required:** Scan all entity fields for price/cost/amount fields
**Decision:**
- If found: Use `BigDecimal` per AGENTS.md Line 148-150
- If not found: No money handling needed in inventory-service ✅
**Auto-Resolution:** Analyze `InventoryItem` and `Reservation` entities for money fields

### 2. Optimistic Locking Strategy
**Analysis Required:** Check for `@Version` annotation usage
**Decision:** 
- Domain model: Keep version as `long` field (business concern)
- JPA entity: Keep `@Version` annotation (technical concern)
- Adapter: Handle version in mapping
**Auto-Resolution:** Implement dual approach (domain field + JPA annotation)

### 3. Transaction Boundaries
**Rule:** `@Transactional` ONLY in application layer use-cases
**Decision:**
- Write operations (create, update, delete): `@Transactional`
- Read operations: NO transaction annotation
- Never in domain or infrastructure layers
**Auto-Resolution:** Apply to all use-cases with persistence writes

### 4. Testing Strategy
**Rule:** Generate tests during refactoring
**Decision:**
- Domain layer: Full unit test coverage (NO Spring)
- Application layer: Use-case tests with mocked repositories
- Infrastructure layer: Optional integration tests
**Auto-Resolution:** Generate domain tests for all business methods

---

## Validation Criteria

Before proceeding to Task 02:
- [ ] All files analyzed and documented
- [ ] All AGENTS.md violations identified
- [ ] Target structure agreed upon
- [ ] Key decisions documented
- [ ] Questions answered

---

## Next Task

**Task 02:** Inventory Service - Domain Layer Creation

---

**Status:** 🤖 READY FOR AI AGENT EXECUTION - Autonomous analysis and code generation enabled

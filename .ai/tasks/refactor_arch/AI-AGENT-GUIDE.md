# AI Agent Execution Guide - Hexagonal Architecture Refactoring

**Target:** E-Commerce Microservices (inventory-service, order-service)  
**Executor:** AI Agent - Fully Autonomous  
**Objective:** Transform to Hexagonal Architecture per AGENTS.md  

---

## 🤖 AI Agent Operation Protocol

### Core Directives

1. **ANALYZE BEFORE GENERATE**
   - Read all task specifications completely
   - Scan existing codebase for current patterns
   - Identify AGENTS.md violations systematically
   - Understand target architecture fully

2. **DECIDE AUTONOMOUSLY**
   - Use AGENTS.md as absolute rulebook
   - Apply existing code patterns where rules don't specify
   - Choose simplest solution when multiple options exist
   - Never invent - derive from codebase + rules

3. **GENERATE SYSTEMATICALLY**
   - Follow file breakdown order strictly
   - Implement complete files (no placeholders)
   - Apply consistent naming conventions
   - Include all imports, annotations, methods

4. **VALIDATE CONTINUOUSLY**
   - Check compilation after each file
   - Run tests incrementally
   - Verify AGENTS.md compliance per file
   - Rollback if validation fails

5. **PROCEED SEQUENTIALLY**
   - Complete Task N before starting Task N+1
   - Use inventory-service as template for order-service
   - Log all decisions for audit trail
   - Self-correct on validation failure

---

## 📋 Execution Sequence

### Phase 1: Inventory Service (Tasks 01-05)

#### Task 01: Analysis
**Input:** Current inventory-service codebase  
**Action:**
- Scan package structure: `com.ecommerce.inventoryservice/**`
- Identify all entity classes and their violations
- Map service layer to AGENTS.md rules
- List all time/money/ID type violations
- Document current vs target architecture gap

**Output:** Structured analysis report with violations mapped to AGENTS.md sections

**Validation:** All current files cataloged, all violations identified

---

#### Task 02: Domain Layer
**Input:** Task 01 analysis + AGENTS.md rules  
**Action:**
- Create `domain/model/` package
  - Generate `InventoryItem.java` (pure business logic, NO JPA)
  - Generate `Reservation.java` (aggregate root)
  - Generate `ReservationItem.java` (value object)
  - Generate `ReservationStatus.java` (enum)
- Create `domain/service/` package
  - Generate `StockService.java` (business calculations)
  - Generate `ReservationService.java` (lifecycle management)
- Create `domain/port/` package
  - Generate `InventoryRepository.java` (interface only)
  - Generate `ReservationRepository.java` (interface only)
- Create `domain/exception/` package
  - Move all exception classes from root to domain

**Key Rules:**
- NO Spring annotations in domain layer
- Use `Instant` for ALL timestamps
- Use `UUID` for ALL IDs
- Immutable value objects
- Business logic in domain entities
- Stateless domain services

**Output:** Complete domain layer (9 files, ~470 lines)

**Validation:**
- Domain compiles without Spring on classpath
- No `LocalDateTime` in domain
- No JPA annotations in domain
- All business methods present

---

#### Task 03: Application Layer
**Input:** Task 02 domain layer  
**Action:**
- Create `application/usecase/` package
  - Generate 6 use-case classes (one per operation)
  - Each use-case: constructor injection of ports, single execute() method
  - Add `@Transactional` only on write operations
- Create `application/dto/` package
  - Generate `ProductQuantity.java` (input DTO)
  - Generate `AvailabilityResult.java` (output DTO)
- Create `config/` package
  - Generate `UseCaseConfig.java` (Spring bean definitions)

**Use-Cases:**
1. `GetInventoryItemUseCase` - read only
2. `CheckAvailabilityUseCase` - read only
3. `CreateReservationUseCase` - @Transactional
4. `GetReservationUseCase` - read only
5. `ConfirmReservationUseCase` - @Transactional
6. `CancelReservationUseCase` - @Transactional

**Key Rules:**
- Use-cases orchestrate, do NOT contain business logic
- Call domain services for business operations
- Depend on domain ports (interfaces), not adapters
- Transaction boundaries ONLY in use-cases

**Output:** Complete application layer (9 files, ~395 lines)

**Validation:**
- Use-cases have NO business logic
- Only write operations have @Transactional
- All dependencies are interfaces (ports)
- Can instantiate use-cases without Spring

---

#### Task 04: Infrastructure Layer
**Input:** Tasks 02 & 03 (domain + application)  
**Action:**
- Create `infrastructure/persistence/entity/` package
  - Rename existing entities with "Entity" suffix
  - Remove ALL business methods from entities
  - Change `LocalDateTime` → `Instant`
  - Keep ONLY JPA annotations and getters/setters
- Create `infrastructure/persistence/repository/` package
  - Rename Spring Data repos with "Jpa" prefix
  - Work with entity classes, not domain models
- Create `infrastructure/persistence/mapper/` package
  - Generate `InventoryItemMapper.java` (domain ↔ entity)
  - Generate `ReservationMapper.java` (domain ↔ entity)
- Create `infrastructure/persistence/adapter/` package
  - Generate `InventoryRepositoryAdapter.java` (implements domain port)
  - Generate `ReservationRepositoryAdapter.java` (implements domain port)

**Transformation:**
```
entity/InventoryItem.java → infrastructure/persistence/entity/InventoryItemEntity.java
  - Remove: isAvailable(), reserve(), release() methods
  - Change: LocalDateTime → Instant
  - Keep: JPA annotations only

repository/InventoryItemRepository.java → infrastructure/persistence/repository/InventoryItemJpaRepository.java
  - Change: extends JpaRepository<InventoryItemEntity, UUID>
```

**Key Rules:**
- Entities are persistence models ONLY (no business logic)
- Mappers do pure data transformation
- Adapters implement domain ports using JPA repositories
- Use `Instant` for all timestamps

**Output:** Complete infrastructure layer (9 files, ~425 lines)

**Validation:**
- Entities have NO business methods
- All timestamps use `Instant`
- Adapters implement correct domain ports
- Mappers tested (domain ↔ entity round-trip)

---

#### Task 05: Web Layer
**Input:** Task 03 application layer (use-cases)  
**Action:**
- Move controller to `web/controller/` package
- Move DTOs to `web/dto/` package
- Update `InventoryController.java`:
  - Replace service injection with 6 use-case injections
  - Update all endpoint methods to call use-cases
  - Add private mapping methods (domain → DTO)
- Update response DTOs:
  - Change `LocalDateTime` → `Instant` in all DTOs
- Update `GlobalExceptionHandler.java`:
  - Add handlers for new domain exceptions

**Controller Pattern:**
```java
@RestController
public class InventoryController {
    private final GetInventoryItemUseCase getInventoryItemUseCase;
    // ... 5 more use-cases
    
    @GetMapping("/api/v1/inventory-items/{productId}")
    public ResponseEntity<InventoryItemResponse> getInventoryItem(@PathVariable UUID productId) {
        InventoryItem item = getInventoryItemUseCase.execute(productId);
        return ResponseEntity.ok(mapToResponse(item));
    }
}
```

**Key Rules:**
- Controller calls use-cases ONLY (no service)
- NO business logic in controller
- Map domain objects to DTOs before returning
- Keep all OpenAPI annotations

**Output:** Updated web layer (4 files, ~280 lines changed)

**Validation:**
- All endpoints still work (integration tests pass)
- Responses use `Instant` format
- API contract unchanged
- Swagger UI still generates

---

### Phase 2: Order Service (Tasks 06-10)

#### Task 06: Analysis
**Same as Task 01, but for order-service**

**Additional Focus:**
- Identify money fields (price, total, subtotal)
- Check for `double`/`float` usage
- Analyze order status transitions
- Document delivery address structure

---

#### Task 07: Domain Layer
**Same pattern as Task 02, but with order-specific models**

**Key Additions:**
1. **Money.java** (value object)
   ```java
   public class Money {
       private final BigDecimal amount;
       private final String currency;
       // Immutable, operations: add(), subtract(), multiply()
   }
   ```

2. **DeliveryAddress.java** (value object)
   ```java
   public class DeliveryAddress {
       private final String street, city, state, postalCode, country;
       // Immutable, validation in constructor
   }
   ```

3. **Order.java** (aggregate root)
   - Method: `calculateTotal(): Money` (sum all item subtotals)
   - Method: `canBeCancelled(): boolean` (business rule)
   - Method: `cancel(): void` (state transition)

4. **OrderPricingService.java** (domain service)
   - Business logic for pricing calculations

5. **OrderValidationService.java** (domain service)
   - State transition validation

**Output:** Domain layer with Money handling (~600 lines)

---

#### Task 08: Application Layer
**Same pattern as Task 03, but with order use-cases**

**Use-Cases:**
1. `CreateOrderUseCase` - @Transactional
2. `GetOrderUseCase` - read only
3. `CancelOrderUseCase` - @Transactional
4. `UpdateOrderStatusUseCase` - @Transactional

**Output:** Application layer (~400 lines)

---

#### Task 09: Infrastructure Layer
**Same pattern as Task 04, but with order entities**

**Key Focus:**
- `OrderEntity` stores `BigDecimal` for money fields
- `MoneyMapper` converts Money ↔ BigDecimal
- Handle `DeliveryAddress` as embedded fields or separate table

**Output:** Infrastructure layer (~500 lines)

---

#### Task 10: Web Layer
**Same pattern as Task 05, but for OrderController**

**Key Focus:**
- Response DTOs return `BigDecimal` as strings (JSON)
- All timestamps as `Instant`

**Output:** Updated web layer (~300 lines)

---

### Phase 3: Validation (Task 11)

**Action:** Run comprehensive automated validation

1. **Compilation Check**
   ```bash
   ./mvnw clean compile
   ```
   Must succeed for both services.

2. **Unit Test Suite**
   ```bash
   ./mvnw test -Dtest="**/domain/**/*Test,**/application/**/*Test"
   ```
   All domain + application tests must pass.

3. **Integration Test Suite**
   ```bash
   ./mvnw verify
   ```
   All integration tests must pass.

4. **Static Analysis**
   ```bash
   # Check for AGENTS.md violations
   grep -r "LocalDateTime" inventory-service/src/main/java/com/ecommerce/inventoryservice/{domain,application}/
   # Should return 0 results
   
   grep -r "double\|float" order-service/src/main/java/**/domain/**/Money.java
   # Should return 0 results
   ```

5. **Architecture Validation**
   - Verify domain has no Spring imports
   - Verify dependency direction (use tools like JDepend if available)

**Success Criteria:** ALL checks pass ✅

**Failure Handling:** Identify failed task, rollback, fix, re-validate

---

## 🎯 AI Agent Decision Framework

### When Specification is Ambiguous

**Example:** "How should I handle reservation expiry time calculation?"

**Resolution:**
1. Check AGENTS.md for explicit rule → Not found
2. Check existing code for pattern → Find `LocalDateTime.now().plusMinutes(15)`
3. Apply transformation: `LocalDateTime` → `Instant`, keep logic
4. Decision: `Instant.now().plus(15, ChronoUnit.MINUTES)`

### When Multiple Approaches Exist

**Example:** "Should Money be a class or record?"

**Resolution:**
1. Check AGENTS.md simplicity rule (Section 4.1)
2. Evaluate options:
   - Record: Simpler, immutable by default
   - Class: More flexible for operations
3. Decision: Use class for operations support, but keep immutable

### When Data is Missing

**Example:** "What currency should Money use?"

**Resolution:**
1. Scan existing code for currency references → None found
2. Check AGENTS.md → No specification
3. Apply simplest solution: Single currency "USD"
4. Document decision: "Using USD as default currency per simplicity principle"

---

## 🔄 Self-Correction Protocol

### If Validation Fails

1. **Identify Failure Point**
   - Which validation check failed?
   - Which file(s) involved?
   - What rule violated?

2. **Root Cause Analysis**
   - Did I misunderstand AGENTS.md rule?
   - Did I miss a file in transformation?
   - Did I introduce a bug?

3. **Correction Strategy**
   - Rollback affected files
   - Re-analyze requirement
   - Regenerate correctly
   - Re-validate

4. **Proceed**
   - If validation passes → continue to next task
   - If still fails → repeat correction

---

## 📊 Progress Tracking

AI Agent should log:

```
TASK_01_ANALYSIS: STARTED - 2026-01-21 10:00:00
TASK_01_ANALYSIS: Files scanned: 23
TASK_01_ANALYSIS: Violations found: 8
TASK_01_ANALYSIS: COMPLETED - 2026-01-21 10:15:00

TASK_02_DOMAIN: STARTED - 2026-01-21 10:15:00
TASK_02_DOMAIN: Created domain/model/InventoryItem.java (120 lines)
TASK_02_DOMAIN: Created domain/service/StockService.java (80 lines)
...
TASK_02_DOMAIN: VALIDATION - Compilation: PASS
TASK_02_DOMAIN: VALIDATION - Domain tests: PASS (12/12)
TASK_02_DOMAIN: COMPLETED - 2026-01-21 11:30:00

TASK_03_APPLICATION: STARTED - 2026-01-21 11:30:00
...
```

---

## ✅ Success Criteria

Refactoring is successful when:

1. ✅ All 11 tasks completed
2. ✅ All validation checks pass
3. ✅ Both services compile and run
4. ✅ All tests pass (unit + integration)
5. ✅ No AGENTS.md rule violations
6. ✅ API contracts unchanged
7. ✅ Architecture follows hexagonal pattern exactly

---

## 🚀 AI Agent: Ready to Execute

**Command:** Begin autonomous refactoring execution

**Starting Point:** [Task 01: INVENTORY-ANALYSIS](01-INVENTORY-ANALYSIS.md)

**Mode:** Autonomous - No human intervention required

---

🤖 **AI AGENT EXECUTION PROTOCOL LOADED** 🤖

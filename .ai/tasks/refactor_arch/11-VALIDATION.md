# Task 11: Final Validation & Testing

**Task ID:** REFACTOR-11  
**Phase:** Validation & Quality Assurance  
**Estimated Time:** 4 hours  
**Dependencies:** Tasks 01-10 ✅  

---

## Objective

Comprehensive validation that ALL AGENTS.md rules are satisfied and both services work correctly with the new Hexagonal Architecture.

---

## Validation Checklist

### Architecture Compliance

#### Hexagonal Architecture (AGENTS.md Section 3)

- [ ] **Layer Structure**
  - [ ] Web layer exists (controller + DTOs)
  - [ ] Application layer exists (use-cases)
  - [ ] Domain layer exists (models + services + ports)
  - [ ] Infrastructure layer exists (entities + adapters)

- [ ] **Dependency Direction**
  - [ ] Domain depends on NOTHING ✅
  - [ ] Application depends ONLY on Domain ✅
  - [ ] Infrastructure depends on Domain + Application ✅
  - [ ] Web depends on Application ✅
  - [ ] NO circular dependencies ✅

- [ ] **Port & Adapter Pattern**
  - [ ] Repository ports defined in domain ✅
  - [ ] Repository adapters in infrastructure ✅
  - [ ] Use-cases depend on ports (not adapters) ✅

---

#### Data Types (AGENTS.md Section 4.2, 4.3)

- [ ] **Money Fields**
  - [ ] All price/amount fields use `BigDecimal` ✅
  - [ ] NO `double` or `float` for money ✅
  - [ ] Proper rounding and precision ✅

- [ ] **Time Fields**
  - [ ] All timestamps use `Instant` ✅
  - [ ] NO `LocalDateTime` in domain/DTOs ✅
  - [ ] UTC timezone configured ✅

- [ ] **ID Fields**
  - [ ] All IDs use `UUID` ✅
  - [ ] Consistent UUID strategy across services ✅

---

#### Business Logic Location (AGENTS.md Section 3.1-3.4)

- [ ] **Controller Layer**
  - [ ] NO business logic ✅
  - [ ] Only input validation & DTO mapping ✅
  - [ ] Returns DTOs (NEVER entities) ✅

- [ ] **Application Layer**
  - [ ] Orchestrates use-cases ✅
  - [ ] Transaction management ✅
  - [ ] NO business rules ✅

- [ ] **Domain Layer**
  - [ ] Contains ALL business invariants ✅
  - [ ] Domain entities have behavior methods ✅
  - [ ] Domain services for complex logic ✅
  - [ ] NO framework annotations ✅

- [ ] **Infrastructure Layer**
  - [ ] JPA entities for persistence ONLY ✅
  - [ ] NO business logic ✅
  - [ ] Mappers between domain ↔ entity ✅

---

### Code Quality

#### Simplicity (AGENTS.md Section 4.1)

- [ ] Simplest working solution used
- [ ] No over-engineering
- [ ] No unnecessary patterns
- [ ] Clear and readable code

#### Naming Conventions

- [ ] Domain models: `Order`, `InventoryItem`
- [ ] JPA entities: `OrderEntity`, `InventoryItemEntity`
- [ ] Use-cases: `CreateOrderUseCase`
- [ ] Adapters: `OrderRepositoryAdapter`
- [ ] Mappers: `OrderMapper`

---

## Testing Strategy

### Unit Tests (Domain Layer)

Run all domain tests WITHOUT Spring context:

```bash
# Inventory Service Domain Tests
./mvnw -pl inventory-service test -Dtest="**/domain/**/*Test"

# Order Service Domain Tests
./mvnw -pl order-service test -Dtest="**/domain/**/*Test"
```

**Expected:**
- [ ] All domain tests pass
- [ ] Tests run in < 2 seconds
- [ ] NO Spring context loaded
- [ ] NO database needed

---

### Unit Tests (Application Layer)

Run use-case tests with mocked repositories:

```bash
# Inventory Service Use Case Tests
./mvnw -pl inventory-service test -Dtest="**/application/**/*Test"

# Order Service Use Case Tests
./mvnw -pl order-service test -Dtest="**/application/**/*Test"
```

**Expected:**
- [ ] All use-case tests pass
- [ ] Tests run in < 3 seconds
- [ ] Repositories mocked with Mockito
- [ ] Domain services used (not mocked)

---

### Integration Tests (Full Stack)

Run integration tests with database:

```bash
# Inventory Service Integration Tests
./mvnw -pl inventory-service test -Dtest="**/*IntegrationTest"

# Order Service Integration Tests
./mvnw -pl order-service test -Dtest="**/*IntegrationTest"
```

**Expected:**
- [ ] All integration tests pass
- [ ] Spring context loads correctly
- [ ] Database operations work
- [ ] API contracts satisfied

---

### Manual API Testing

#### Inventory Service

```bash
# 1. Get inventory item
curl http://localhost:8081/api/v1/inventory-items/{productId}

# 2. Check availability
curl -X POST http://localhost:8081/api/v1/inventory-items/availability-check \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productId":"...","quantity":5}]}'

# 3. Create reservation
curl -X POST http://localhost:8081/api/v1/inventory-reservations \
  -H "Content-Type: application/json" \
  -d '{"orderId":"...","items":[{"productId":"...","quantity":5}]}'

# 4. Confirm reservation
curl -X POST http://localhost:8081/api/v1/inventory-reservations/{id}/confirm

# 5. Cancel reservation
curl -X POST http://localhost:8081/api/v1/inventory-reservations/{id}/cancel
```

**Verify:**
- [ ] All endpoints respond correctly
- [ ] Response DTOs use `Instant` format
- [ ] Error handling works
- [ ] Business rules enforced

---

#### Order Service

```bash
# 1. Create order
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"...","items":[...],"deliveryAddress":{...}}'

# 2. Get order
curl http://localhost:8080/api/v1/orders/{id}

# 3. Cancel order
curl -X POST http://localhost:8080/api/v1/orders/{id}/cancel

# 4. Update status
curl -X PUT http://localhost:8080/api/v1/orders/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status":"CONFIRMED"}'
```

**Verify:**
- [ ] Money fields return as strings with decimal precision
- [ ] Timestamps in ISO-8601 format
- [ ] Business rule violations return 409 Conflict

---

## Code Review Checklist

### Inventory Service

- [ ] `domain/` package has NO Spring annotations
- [ ] `domain/model/InventoryItem.java` has business methods
- [ ] `domain/port/InventoryRepository.java` is an interface
- [ ] `application/usecase/` classes are pure Java (except `@Transactional`)
- [ ] `infrastructure/persistence/entity/` has JPA entities only
- [ ] `infrastructure/persistence/adapter/` implements domain ports
- [ ] `infrastructure/persistence/mapper/` converts domain ↔ entity
- [ ] `web/controller/` uses use-cases (not service)
- [ ] All timestamps use `Instant`

---

### Order Service

- [ ] `domain/model/Money.java` uses `BigDecimal`
- [ ] `domain/model/Order.java` has `calculateTotal()` method
- [ ] `domain/service/OrderPricingService.java` handles calculations
- [ ] `application/usecase/CreateOrderUseCase.java` orchestrates
- [ ] `infrastructure/persistence/entity/OrderEntity.java` has NO business logic
- [ ] `infrastructure/persistence/mapper/OrderMapper.java` handles Money conversion
- [ ] `web/controller/OrderController.java` returns OrderResponse DTOs
- [ ] All money fields use `BigDecimal`
- [ ] All timestamps use `Instant`

---

## Documentation Validation

- [ ] AGENTS.md rules all satisfied
- [ ] DECISIONS.md updated with refactoring decisions
- [ ] README.md reflects new architecture
- [ ] OpenAPI specs still valid
- [ ] All TODO comments removed

---

## Performance Validation

### Startup Time

```bash
time ./mvnw spring-boot:run -pl inventory-service
time ./mvnw spring-boot:run -pl order-service
```

**Expected:**
- [ ] Inventory service starts in < 10 seconds
- [ ] Order service starts in < 10 seconds
- [ ] NO errors in startup logs

---

### Build Time

```bash
time ./mvnw clean install
```

**Expected:**
- [ ] Full build completes in < 60 seconds
- [ ] All tests pass
- [ ] NO warnings about deprecated APIs

---

## Rollback Plan

If validation fails:

1. **Identify Issue**
   - Which AGENTS.md rule violated?
   - Which service affected?
   - Which layer has the problem?

2. **Fix Strategy**
   - Domain issue → Fix in Task 02/07
   - Application issue → Fix in Task 03/08
   - Infrastructure issue → Fix in Task 04/09
   - Web issue → Fix in Task 05/10

3. **Revalidate**
   - Run relevant tests
   - Check specific violation
   - Proceed to next check

---

## Success Criteria

All items checked means:

✅ Hexagonal Architecture fully implemented  
✅ All AGENTS.md rules satisfied  
✅ Clean separation of concerns  
✅ Unit-testable domain & application layers  
✅ No breaking changes to API contracts  
✅ Both services working correctly  

---

## AI Agent Automated Validation

### Automated Checks (Must ALL Pass)

- [ ] **Compilation:** Both services compile without errors
  ```bash
  ./mvnw clean compile -pl inventory-service
  ./mvnw clean compile -pl order-service
  ```

- [ ] **Unit Tests:** All domain & application tests pass
  ```bash
  ./mvnw test -pl inventory-service -Dtest="**/domain/**/*Test,**/application/**/*Test"
  ./mvnw test -pl order-service -Dtest="**/domain/**/*Test,**/application/**/*Test"
  ```

- [ ] **Integration Tests:** Full stack tests pass
  ```bash
  ./mvnw verify -pl inventory-service
  ./mvnw verify -pl order-service
  ```

- [ ] **Static Analysis:** No AGENTS.md rule violations detected
  - Grep for `LocalDateTime` in domain/application layers (should be 0)
  - Grep for `double` or `float` in money fields (should be 0)
  - Check domain packages have no Spring annotations

- [ ] **Architecture Validation:** Dependencies point correct direction
  - Domain imports: NO infrastructure, NO Spring
  - Application imports: ONLY domain
  - Infrastructure imports: Can import domain + application

### Success Criteria (Automated)

All items checked → AI Agent execution successful ✅
Any item fails → Rollback and retry ❌

---

**AI Agent Validation Mode:** Fully automated - no human review required for technical compliance.

---

## Next Steps (Post-Validation)

### If All Validations Pass ✅
1. **Mark Refactoring Complete**
2. **Generate Summary Report:**
   - Files created/modified count
   - Test coverage metrics
   - AGENTS.md compliance status
   - Architecture diagram (updated)
3. **Log for Human Review:**
   - Any ambiguous decisions made
   - Pattern deviations (if any)
   - Performance impact (if measured)

### If Validations Fail ❌
1. **Identify Failed Check**
2. **Analyze Root Cause**
3. **Rollback Affected Files**
4. **Re-execute Failed Task**
5. **Re-run Validation**

### Post-Execution (Human Review Optional)
1. Review generated summary report
2. Verify business logic preserved
3. Deploy to test environment
4. Monitor for issues

---

**AI Agent Post-Execution:** Generate comprehensive report for human review

---

**Status:** 🔄 READY TO EXECUTE (AI Agent: Automated validation after Tasks 01-10)

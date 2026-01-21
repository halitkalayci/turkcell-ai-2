# Hexagonal Architecture Refactoring - AI Agent Execution Plan

**Project:** E-Commerce Microservices  
**Services:** order-service, inventory-service  
**Executor:** AI Agent (Autonomous)  
**Goal:** Full AGENTS.md Compliance  
**Mode:** Fully Autonomous with Continuous Validation  

---

## 🤖 AI Agent Execution Overview

This refactoring will be executed by an AI Agent autonomously. The agent will:

- ✅ Analyze existing codebase thoroughly
- ✅ Make architectural decisions based on AGENTS.md rules
- ✅ Generate code following hexagonal architecture patterns
- ✅ Validate output at each step automatically
- ✅ Proceed sequentially without human intervention
- ✅ Self-correct if validation fails

**NO HUMAN APPROVAL REQUIRED** between tasks. Agent operates autonomously.

---

## 📋 Quick Reference

| Task | Service | Phase | Time | Status |
|------|---------|-------|------|--------|
| 00 | Both | Overview | - | ✅ Complete |
| 01 | Inventory | Analysis | 2h | ⏸️ Pending |
| 02 | Inventory | Domain | 4h | ⏸️ Pending |
| 03 | Inventory | Application | 5h | ⏸️ Pending |
| 04 | Inventory | Infrastructure | 6h | ⏸️ Pending |
| 05 | Inventory | Web | 3h | ⏸️ Pending |
| 06 | Order | Analysis | 2h | ⏸️ Pending |
| 07 | Order | Domain | 5h | ⏸️ Pending |
| 08 | Order | Application | 5h | ⏸️ Pending |
| 09 | Order | Infrastructure | 4h | ⏸️ Pending |
| 10 | Order | Web | 2h | ⏸️ Pending |
| 11 | Both | Validation | 4h | ⏸️ Pending |

**Total:** 42 hours (1 week for 1 developer)

---

## 🎯 High-Level Plan

### Phase 1: Inventory Service (20 hours)
1. Analyze current state and violations
2. Build domain layer (pure business logic)
3. Build application layer (use-cases)
4. Refactor infrastructure (adapters)
5. Update web layer (controllers)

### Phase 2: Order Service (18 hours)
6. Repeat same pattern for order service
7. Handle Money value object (`BigDecimal`)
8. Handle DeliveryAddress value object

### Phase 3: Validation (4 hours)
9. Comprehensive testing
10. AGENTS.md compliance check
11. Performance validation

---

## 🚀 AI Agent Execution Strategy

### Sequential Execution (REQUIRED)
AI Agent MUST execute tasks 01 → 11 in strict order.
- ✅ Clear dependencies
- ✅ Each task builds on previous
- ✅ Validation at each step
- ✅ No human intervention needed

### Execution Mode: Fully Autonomous
- **Phase 1:** Inventory Service (Tasks 01-05) - Complete end-to-end
- **Phase 2:** Order Service (Tasks 06-10) - Apply learned patterns
- **Phase 3:** Validation (Task 11) - Automated checks
- ✅ Self-validating at each step
- ✅ Pattern replication from inventory to order
- ✅ Rollback capability if validation fails

### AI Agent Decision Framework
1. Analyze current state from codebase
2. Apply AGENTS.md rules strictly
3. Make architectural decisions based on analysis
4. Generate code following patterns
5. Validate output automatically
6. Proceed to next task if validation passes

---

## 📊 Key Milestones

### Milestone 1: Inventory Domain Complete (Task 02)
- [ ] Pure domain models created
- [ ] Business logic in domain
- [ ] NO framework dependencies
- [ ] Domain tests passing

**Exit Criteria:** Domain layer compiles without Spring on classpath

---

### Milestone 2: Inventory Service Complete (Task 05)
- [ ] All layers implemented
- [ ] Integration tests passing
- [ ] API contracts satisfied
- [ ] No breaking changes

**Exit Criteria:** Inventory service deployable and functional

---

### Milestone 3: Order Service Complete (Task 10)
- [ ] Same architecture as inventory
- [ ] Money handling correct (`BigDecimal`)
- [ ] All tests passing

**Exit Criteria:** Order service deployable and functional

---

### Milestone 4: Full Compliance (Task 11)
- [ ] All AGENTS.md rules satisfied
- [ ] Both services tested
- [ ] Documentation updated

**Exit Criteria:** Production-ready refactored services

---

## 📝 AI Agent Execution Checklist

### Before Starting Any Task
- [ ] Parse task file completely
- [ ] Analyze current codebase state
- [ ] Load target architecture patterns
- [ ] Identify all file operations needed

### During Task Execution
- [ ] Follow file breakdown strictly
- [ ] Generate files in specified order
- [ ] Run validation checks incrementally
- [ ] Track all modifications for rollback

### After Completing Task
- [ ] All validation criteria met automatically
- [ ] All tests passing (run automated test suite)
- [ ] Code compiles successfully
- [ ] Log any pattern deviations
- [ ] Mark task complete and proceed to next

---

## 🔧 Technical Requirements

### Development Environment
- Java 21
- Maven 3.8+
- Spring Boot 3.X
- H2 Database (in-memory)
- IDE with good refactoring support

### Build Commands
```bash
# Build all
./mvnw clean install

# Build specific service
./mvnw clean install -pl inventory-service
./mvnw clean install -pl order-service

# Run tests
./mvnw test

# Run specific service
./mvnw spring-boot:run -pl inventory-service
./mvnw spring-boot:run -pl order-service
```

---

## 🚨 Critical Rules (DO NOT VIOLATE)

### 1. Dependency Direction
```
Web → Application → Domain ← Infrastructure
```
- Domain depends on NOTHING
- NEVER import infrastructure in domain

### 2. Data Types
- Money: `BigDecimal` ONLY
- Time: `Instant` ONLY
- IDs: `UUID` ONLY

### 3. Business Logic Location
- Business rules → Domain layer
- Orchestration → Application layer
- Persistence → Infrastructure layer
- Validation/Mapping → Web layer

### 4. No Inventing
- Stick to OpenAPI contracts
- No new features during refactoring
- No schema changes
- Ask if uncertain

---

## 📚 Documentation to Update

After refactoring:
- [ ] Update DECISIONS.md with architecture decisions
- [ ] Update service README files
- [ ] Document new package structure
- [ ] Update developer onboarding guide

---

## 🎓 Learning Resources

For team members unfamiliar with patterns:

1. **Hexagonal Architecture**
   - Read: "Hexagonal Architecture" by Alistair Cockburn
   - Watch: "Clean Architecture" talks

2. **Domain-Driven Design**
   - Entities vs Value Objects
   - Aggregate Roots
   - Domain Services

3. **Port & Adapter Pattern**
   - Dependency Inversion Principle
   - Interface Segregation

---

## ❓ AI Agent Decision Rules

### Q: Can I skip domain services if logic is simple?
**A:** NO. Always create domain services even for simple logic. Maintains consistency.

### Q: Should I generate tests during refactoring?
**A:** YES. Generate domain and application layer tests during implementation. Infrastructure tests optional.

### Q: What if I detect a bug in existing business logic?
**A:** LOG IT but do NOT fix during refactoring. Focus on structural changes only. One concern at a time.

### Q: Can I improve the API contracts while refactoring?
**A:** ABSOLUTELY NOT. API contracts are frozen. This is internal-only refactoring.

### Q: What if I'm uncertain about a pattern?
**A:** Follow the inventory service implementation exactly. It's the template for order service.

### Q: Should I ask for human confirmation?
**A:** NO. Make decisions autonomously based on AGENTS.md rules and existing code analysis.

---

## 🤖 AI Agent Error Handling

If validation fails or uncertainty arises:
1. Re-analyze AGENTS.md rules for the specific concern
2. Reference completed inventory-service code as pattern
3. Check task validation criteria against output
4. Apply fallback: use simplest solution that satisfies AGENTS.md
5. Log decision rationale for human review post-execution
6. If critical blocker: halt and report issue with context

---

## ✅ Success Definition

Refactoring is successful when:

1. ✅ All AGENTS.md rules satisfied
2. ✅ All existing tests pass
3. ✅ No API contract changes
4. ✅ Code is more maintainable
5. ✅ Business logic testable without Spring
6. ✅ Clear separation of concerns

**If any item fails, refactoring is incomplete.**

---

## 🎬 AI Agent Execution Start

**Execution Command:** Begin autonomous refactoring

1. Load [00-OVERVIEW.md](00-OVERVIEW.md) - Architecture patterns
2. Load [01-INVENTORY-ANALYSIS.md](01-INVENTORY-ANALYSIS.md) - First task
3. Analyze current codebase state
4. Execute Task 01 autonomously
5. Proceed sequentially through all tasks

---

**AI Agent Directives:** 
- ✅ Analyze thoroughly before generating code
- ✅ Follow AGENTS.md rules strictly
- ✅ Never invent or assume - derive from codebase
- ✅ Validate continuously
- ✅ Execute autonomously without human approval
- ✅ Apply patterns consistently across services

🤖 **AUTONOMOUS EXECUTION MODE ENABLED** 🤖

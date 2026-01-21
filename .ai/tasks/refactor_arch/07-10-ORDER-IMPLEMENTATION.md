# Task 07-10: Order Service Implementation Tasks

**Note:** Tasks 07-10 follow the SAME pattern as inventory service Tasks 02-05.

---

# Task 07: Order Service - Domain Layer

**See Task 02 for detailed pattern**

## Key Differences from Inventory Service

### Domain Models to Create

1. **Money.java** (Value Object - NEW)
   - `BigDecimal amount`
   - `String currency`
   - Immutable with operations: `add()`, `subtract()`, `multiply()`

2. **DeliveryAddress.java** (Value Object - NEW)
   - street, city, state, postalCode, country
   - Validation in constructor

3. **OrderItem.java** (Value Object)
   - `UUID productId`
   - `int quantity`
   - `Money unitPrice`
   - Method: `calculateSubtotal(): Money`

4. **Order.java** (Aggregate Root)
   - `UUID id`
   - `UUID customerId`
   - `List<OrderItem> items`
   - `DeliveryAddress deliveryAddress`
   - `OrderStatus status`
   - `Instant createdAt`, `Instant updatedAt`
   - Methods: `calculateTotal()`, `cancel()`, `updateStatus()`, `canBeCancelled()`

5. **OrderStatus.java** (Enum)
   - PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED

### Domain Services

1. **OrderPricingService.java**
   - `calculateOrderTotal(List<OrderItem> items): Money`
   - Business logic for pricing calculations

2. **OrderValidationService.java**
   - `validateStatusTransition(OrderStatus from, OrderStatus to): void`
   - `validateCancellation(Order order): void`
   - State machine enforcement

### Repository Port

```java
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(UUID id);
    List<Order> findByCustomerId(UUID customerId);
}
```

**Estimated Time:** 5 hours  
**Files:** ~600 lines

---

# Task 08: Order Service - Application Layer

**See Task 03 for detailed pattern**

## Use-Cases to Create

1. **CreateOrderUseCase**
   - Validate items
   - Calculate total via domain service
   - Create Order aggregate
   - Save via repository
   - **@Transactional**

2. **GetOrderUseCase**
   - Retrieve by ID
   - Map to application DTO

3. **CancelOrderUseCase**
   - Load order
   - Validate cancellation (domain service)
   - Call `order.cancel()`
   - Save
   - **@Transactional**

4. **UpdateOrderStatusUseCase**
   - Load order
   - Validate transition (domain service)
   - Update status
   - Save
   - **@Transactional**

### Configuration

**UseCaseConfig.java** - Define all use-case beans

**Estimated Time:** 5 hours  
**Files:** ~400 lines

---

# Task 09: Order Service - Infrastructure Layer

**See Task 04 for detailed pattern**

## Entities to Create

1. **OrderEntity.java**
   - JPA entity with relationships
   - Use `BigDecimal` for money storage
   - Use `Instant` for timestamps
   - NO business methods

2. **OrderItemEntity.java**
   - `@ManyToOne` to OrderEntity
   - `BigDecimal price`, `BigDecimal subtotal`
   - Integer quantity

### JPA Repositories

1. **OrderJpaRepository**
   - Extends `JpaRepository<OrderEntity, UUID>`
   - Query methods: `findByCustomerId(UUID)`

2. **OrderItemJpaRepository** (if needed)

### Mappers

1. **OrderMapper**
   - Convert `Order` ↔ `OrderEntity`
   - Handle `Money` ↔ `BigDecimal`
   - Handle `DeliveryAddress` ↔ entity fields
   - Handle `OrderItem` list ↔ `OrderItemEntity` list

2. **MoneyMapper** (helper)
   ```java
   public class MoneyMapper {
       public BigDecimal toDecimal(Money money) {
           return money.getAmount();
       }
       
       public Money toDomain(BigDecimal amount, String currency) {
           return new Money(amount, currency);
       }
   }
   ```

### Adapter

**OrderRepositoryAdapter.java**
- Implements `domain.port.OrderRepository`
- Uses `OrderJpaRepository` + `OrderMapper`

**Estimated Time:** 4 hours  
**Files:** ~500 lines

---

# Task 10: Order Service - Web Layer

**See Task 05 for detailed pattern**

## Controller Updates

**OrderController.java**
- Inject 4 use-cases
- Map web DTOs ↔ application DTOs
- Handle Money serialization in responses

### DTO Updates

1. **OrderResponse.java**
   - Use `BigDecimal` for total (serialized as string)
   - Use `Instant` for timestamps
   - Nested `OrderItemResponse` with `BigDecimal` prices

2. **OrderRequest.java**
   - Keep existing structure
   - Ensure `BigDecimal` for prices

### Exception Handling

Update `GlobalExceptionHandler` for new exceptions

**Estimated Time:** 2 hours  
**Files:** ~300 lines

---

## Combined Validation for Order Service

After completing Tasks 07-10:

- [ ] Domain layer: NO Spring, uses `BigDecimal` & `Instant`
- [ ] Application layer: Use-cases with `@Transactional`
- [ ] Infrastructure: Adapters + Mappers working
- [ ] Web: Controller using use-cases, DTOs correct
- [ ] All tests passing
- [ ] Money calculations accurate
- [ ] State transitions enforced

---

**Total Order Service Refactoring:** 16 hours, ~1800 lines of code

---

**Status:** 📋 AI AGENT REFERENCE TASKS - Follow inventory service pattern exactly

**AI Agent Directive:** Apply learned patterns from inventory-service (Tasks 02-05) to order-service implementation. All architectural decisions already made in inventory service should be replicated here with order-specific business logic.

# Task 06-10: Order Service Refactoring

**Services:** order-service  
**Phase:** Complete Hexagonal Architecture Implementation  
**Estimated Total Time:** 18 hours  

---

## Overview

Order service refactoring follows the SAME pattern as inventory service, but with order-specific business logic. Tasks 06-10 mirror Tasks 01-05 structure.

---

## Task Structure

### Task 06: Order Service - Analysis (2 hours)
### Task 07: Order Service - Domain Layer (5 hours)
### Task 08: Order Service - Application Layer (5 hours)
### Task 09: Order Service - Infrastructure Layer (4 hours)
### Task 10: Order Service - Web Layer (2 hours)

---

# Task 06: Order Service - Current State Analysis

**Task ID:** REFACTOR-06  
**Service:** order-service  
**Phase:** Analysis  
**Dependencies:** None (can start after inventory service Task 01)  

---

## Current Package Structure

```
com.ecommerce.orderservice/
├── OrderServiceApplication.java
├── config/
│   └── OpenApiConfig.java
├── controller/
│   └── OrderController.java
├── dto/
│   ├── request/
│   │   ├── OrderRequest.java
│   │   └── UpdateStatusRequest.java
│   └── response/
│       ├── CancelOrderResponse.java
│       └── OrderResponse.java
├── entity/
│   ├── Order.java
│   ├── OrderItem.java
│   └── OrderStatus.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── OrderNotFoundException.java
│   └── InvalidOrderStateException.java
├── repository/
│   ├── OrderRepository.java
│   └── OrderItemRepository.java
└── service/
    ├── OrderService.java (interface)
    └── OrderServiceImpl.java
```

---

## Files Analysis

### 1. Entity Layer Issues

#### `entity/Order.java`
**Violations:**
- Uses `LocalDateTime` instead of `Instant` ❌ (AGENTS.md Line 151)
- May have `double/float` for totals ❌ (should be `BigDecimal`)
- Business logic mixed with JPA annotations ❌
- Methods like `calculateTotal()`, `canBeCancelled()` are domain logic ❌

**Action Required:**
- Create `domain.model.Order` (pure domain)
- Create `infrastructure.persistence.entity.OrderEntity` (JPA only)
- Use `BigDecimal` for money values
- Use `Instant` for timestamps

---

#### `entity/OrderItem.java`
**Violations:**
- Likely uses `double/float` for `price`, `subtotal` ❌
- Lacks value object characteristics
- JPA annotations mixed with value calculations

**Action Required:**
- Create `domain.model.OrderItem` as value object
- Use `BigDecimal` for all money fields
- Immutable after creation

---

#### `entity/OrderStatus.java`
**Status:** Clean enum ✅
**Action:** Move to `domain.model.OrderStatus`

---

### 2. Service Layer Issues

Similar to inventory service:
- `OrderServiceImpl` contains business logic ❌
- Direct repository usage (no ports) ❌
- Transaction management mixed with logic ❌
- NOT unit-testable without Spring ❌

---

### 3. Key Business Rules to Extract

From analyzing order service:

**Order Creation:**
- Validate customer ID
- Calculate order total from items
- Initialize status as PENDING
- Generate order ID (UUID)

**Order Cancellation:**
- Only PENDING or CONFIRMED orders can be cancelled
- Cannot cancel SHIPPED or DELIVERED orders
- Status transition: * → CANCELLED

**Order Status Updates:**
- Valid transitions only
- PENDING → CONFIRMED → SHIPPED → DELIVERED
- Any state → CANCELLED (if allowed)

**Money Calculations:**
- Item subtotal = price × quantity
- Order total = sum of all item subtotals
- ALL use `BigDecimal` with proper rounding

---

## Target Package Structure

```
com.ecommerce.orderservice/
├── OrderServiceApplication.java
├── web/
│   ├── controller/
│   │   └── OrderController.java
│   └── dto/
│       ├── request/
│       └── response/
├── application/
│   ├── usecase/
│   │   ├── CreateOrderUseCase.java
│   │   ├── GetOrderUseCase.java
│   │   ├── CancelOrderUseCase.java
│   │   └── UpdateOrderStatusUseCase.java
│   └── port/
│       └── (optional input ports)
├── domain/
│   ├── model/
│   │   ├── Order.java           [Pure domain]
│   │   ├── OrderItem.java       [Value object]
│   │   ├── OrderStatus.java     [Enum]
│   │   ├── DeliveryAddress.java [Value object - NEW]
│   │   └── Money.java           [Value object - NEW]
│   ├── service/
│   │   ├── OrderPricingService.java
│   │   └── OrderValidationService.java
│   ├── exception/
│   │   ├── OrderNotFoundException.java
│   │   └── InvalidOrderStateException.java
│   └── port/
│       └── OrderRepository.java  [Interface]
├── infrastructure/
│   ├── persistence/
│   │   ├── entity/
│   │   │   ├── OrderEntity.java
│   │   │   └── OrderItemEntity.java
│   │   ├── repository/
│   │   │   ├── OrderJpaRepository.java
│   │   │   └── OrderItemJpaRepository.java
│   │   ├── adapter/
│   │   │   └── OrderRepositoryAdapter.java
│   │   └── mapper/
│   │       ├── OrderMapper.java
│   │       └── OrderItemMapper.java
│   └── config/
│       └── OpenApiConfig.java
└── config/
    └── UseCaseConfig.java
```

---

## Critical Data Type Changes

### Money Fields

All price/amount fields MUST use `BigDecimal`:

| Field | Current Type | Target Type | Reason |
|-------|--------------|-------------|--------|
| OrderItem.price | ? | `BigDecimal` | AGENTS.md Line 148 |
| OrderItem.subtotal | ? | `BigDecimal` | AGENTS.md Line 148 |
| Order.total | ? | `BigDecimal` | AGENTS.md Line 148 |

### Time Fields

All timestamp fields MUST use `Instant`:

| Field | Current Type | Target Type | Reason |
|-------|--------------|-------------|--------|
| Order.createdAt | `LocalDateTime` | `Instant` | AGENTS.md Line 151 |
| Order.updatedAt | `LocalDateTime` | `Instant` | AGENTS.md Line 151 |

---

## Domain Model Design

### `Money` Value Object (NEW)
```java
public class Money {
    private final BigDecimal amount;
    private final String currency;  // "USD", "EUR", etc.
    
    // Immutable, value equality
    // Operations: add(), subtract(), multiply()
}
```

### `DeliveryAddress` Value Object (NEW)
```java
public class DeliveryAddress {
    private final String street;
    private final String city;
    private final String state;
    private final String postalCode;
    private final String country;
    
    // Immutable, validation in constructor
}
```

### `OrderItem` Value Object
```java
public class OrderItem {
    private final UUID productId;
    private final int quantity;
    private final Money unitPrice;
    
    public Money calculateSubtotal() {
        return unitPrice.multiply(quantity);
    }
}
```

### `Order` Aggregate Root
```java
public class Order {
    private final UUID id;
    private final UUID customerId;
    private final List<OrderItem> items;
    private final DeliveryAddress deliveryAddress;
    private OrderStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    
    // Business methods
    public Money calculateTotal() {...}
    public void cancel() {...}
    public void updateStatus(OrderStatus newStatus) {...}
    public boolean canBeCancelled() {...}
}
```

---

## AI Agent Decision Rules (AUTO-RESOLVE)

### 1. Currency Support
**Analysis Required:** Check Order/OrderItem entities for currency fields
**Decision:** 
- Start with single currency ("USD") - simplest solution
- Store currency as static constant
- Future expansion possible if needed
**Auto-Resolution:** Implement Money value object with "USD" default

### 2. Order Total Storage Strategy
**Analysis Required:** Balance between calculation vs storage
**Decision:**
- Domain: Calculate on-demand via `calculateTotal()` method
- Entity: Store in database for query performance
- Mapper: Sync calculated value to entity before save
**Auto-Resolution:** Implement calculated property in domain, stored field in entity

### 3. Status Transition Enforcement
**Rule:** Strict state machine for order status changes
**Decision:**
- Create `OrderValidationService` in domain layer
- Method: `validateStatusTransition(from, to)` throws if invalid
- Valid transitions:
  - PENDING → CONFIRMED, CANCELLED
  - CONFIRMED → SHIPPED, CANCELLED
  - SHIPPED → DELIVERED
  - DELIVERED → (terminal)
  - CANCELLED → (terminal)
**Auto-Resolution:** Implement domain service with transition rules

### 4. External Service Integration (Inventory)
**Analysis Required:** Check if order service calls inventory service
**Decision:**
- If calls exist: Create infrastructure adapter `InventoryClient`
- Port interface in domain: `StockReservationPort`
- Implementation in infrastructure layer
- For refactoring: Keep existing integration, wrap in adapter
**Auto-Resolution:** Analyze existing service calls, wrap in port/adapter pattern

---

## Identified Violations

1. ❌ No hexagonal architecture
2. ❌ Money fields not using `BigDecimal`
3. ❌ Time fields using `LocalDateTime` instead of `Instant`
4. ❌ Business logic in service implementation
5. ❌ No domain layer
6. ❌ No application layer (use-cases)
7. ❌ Entity = Domain Model = Persistence Model

---

## Next Task

**Task 07:** Order Service - Domain Layer Creation

---

**Status:** ⏸️ READY FOR AI AGENT EXECUTION (Execute after inventory service Tasks 01-05)

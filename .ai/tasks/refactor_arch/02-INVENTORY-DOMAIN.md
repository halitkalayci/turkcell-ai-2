# Task 02: Inventory Service - Domain Layer Creation

**Task ID:** REFACTOR-02  
**Service:** inventory-service  
**Phase:** Implementation - Domain Layer  
**Estimated Time:** 4 hours  
**Dependencies:** Task 01 (Analysis) ✅  

---

## Objective

Create pure domain layer with business logic, zero framework dependencies, following Hexagonal Architecture principles.

---

## Files to Create

### Package: `domain.model`

#### 1. `InventoryItem.java` (Domain Model)
**Path:** `com.ecommerce.inventoryservice.domain.model.InventoryItem`

**Requirements:**
- Pure Java class (NO JPA annotations)
- Immutable ID (UUID)
- Business behavior methods
- Value validation in constructor
- Use `Instant` for timestamps

**Business Methods to Implement:**
```java
- isAvailable(int requestedQuantity): boolean
- reserve(int quantity): void
- release(int quantity): void
- restock(int quantity): void
- validateQuantities(): void // invariant check
```

**Properties:**
- `UUID productId` (immutable)
- `int availableQuantity`
- `int reservedQuantity`
- `int totalQuantity`
- `Instant lastUpdatedAt`
- `long version` (for optimistic locking)

**Invariants:**
- `totalQuantity = availableQuantity + reservedQuantity` (ALWAYS)
- Quantities cannot be negative
- `productId` cannot be null

---

#### 2. `Reservation.java` (Domain Model)
**Path:** `com.ecommerce.inventoryservice.domain.model.Reservation`

**Requirements:**
- Pure Java class (NO JPA annotations)
- Immutable after creation (except status)
- Use `Instant` for all time fields

**Business Methods:**
```java
- isExpired(): boolean
- confirm(): void
- cancel(): void
- canBeConfirmed(): boolean
- canBeCancelled(): boolean
```

**Properties:**
- `UUID id`
- `UUID orderId`
- `List<ReservationItem> items`
- `ReservationStatus status`
- `Instant createdAt`
- `Instant expiresAt`

**Business Rules:**
- Cannot confirm expired reservation
- Cannot cancel confirmed reservation
- Expiration check based on current time vs `expiresAt`

---

#### 3. `ReservationItem.java` (Value Object)
**Path:** `com.ecommerce.inventoryservice.domain.model.ReservationItem`

**Requirements:**
- Immutable value object
- No identity (equals by value)
- Validation in constructor

**Properties:**
- `UUID productId`
- `int quantity`

**Validations:**
- `quantity > 0`
- `productId` not null

---

#### 4. `ReservationStatus.java` (Enum)
**Path:** `com.ecommerce.inventoryservice.domain.model.ReservationStatus`

**Values:**
```java
PENDING,
CONFIRMED,
CANCELLED,
EXPIRED
```

**Methods:**
```java
- isTerminal(): boolean  // true for CONFIRMED, CANCELLED, EXPIRED
```

---

### Package: `domain.service`

#### 5. `StockService.java` (Domain Service)
**Path:** `com.ecommerce.inventoryservice.domain.service.StockService`

**Purpose:** Complex business logic that doesn't fit in a single entity

**Methods:**
```java
- checkMultipleAvailability(Map<UUID, Integer> productQuantities, 
                           List<InventoryItem> items): boolean
  
- calculateReservationExpiry(Instant currentTime): Instant
  // Returns currentTime + 15 minutes

- validateStockReservation(List<ReservationItem> items,
                          Map<UUID, InventoryItem> inventory): void
  // Throws InsufficientStockException if any item unavailable
```

**Requirements:**
- Pure business logic
- NO repository dependencies
- NO framework annotations
- Stateless

---

#### 6. `ReservationService.java` (Domain Service)
**Path:** `com.ecommerce.inventoryservice.domain.service.ReservationService`

**Purpose:** Reservation lifecycle management

**Methods:**
```java
- createReservation(UUID orderId, List<ReservationItem> items): Reservation
  // Creates new reservation with PENDING status

- validateReservationCancellation(Reservation reservation): void
  // Throws exception if cannot be cancelled

- validateReservationConfirmation(Reservation reservation): void
  // Throws exception if cannot be confirmed or expired
```

---

### Package: `domain.port`

#### 7. `InventoryRepository.java` (Port Interface)
**Path:** `com.ecommerce.inventoryservice.domain.port.InventoryRepository`

**Purpose:** Outbound port for inventory persistence

```java
public interface InventoryRepository {
    Optional<InventoryItem> findByProductId(UUID productId);
    List<InventoryItem> findByProductIds(List<UUID> productIds);
    InventoryItem save(InventoryItem item);
    void saveAll(List<InventoryItem> items);
}
```

**Requirements:**
- Works with DOMAIN models (not JPA entities)
- No Spring Data inheritance
- Adapter will implement this

---

#### 8. `ReservationRepository.java` (Port Interface)
**Path:** `com.ecommerce.inventoryservice.domain.port.ReservationRepository`

**Purpose:** Outbound port for reservation persistence

```java
public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(UUID id);
    Optional<Reservation> findByOrderId(UUID orderId);
    List<Reservation> findExpiredReservations(Instant currentTime);
}
```

---

### Package: `domain.exception`

#### 9. Exception Classes
**Path:** `com.ecommerce.inventoryservice.domain.exception.*`

**Move existing exceptions:**
- `InsufficientStockException.java`
- `ProductNotFoundException.java`
- `ReservationConflictException.java`
- `ReservationNotFoundException.java` [NEW]
- `InvalidReservationStateException.java` [NEW]

**New Exception:**
```java
public class InvalidReservationStateException extends RuntimeException {
    public InvalidReservationStateException(String message) {
        super(message);
    }
}
```

---

## Implementation Guidelines

### 1. Domain Model Pattern
```java
public class InventoryItem {
    private final UUID productId;
    private int availableQuantity;
    private int reservedQuantity;
    private Instant lastUpdatedAt;
    private long version;

    // Constructor with validation
    public InventoryItem(UUID productId, int availableQuantity, 
                         int reservedQuantity, long version) {
        validateNotNull(productId, "Product ID cannot be null");
        validateNonNegative(availableQuantity, "Available quantity");
        validateNonNegative(reservedQuantity, "Reserved quantity");
        
        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
        this.version = version;
        this.lastUpdatedAt = Instant.now();
        
        validateInvariant();
    }

    // Business method
    public void reserve(int quantity) {
        if (!isAvailable(quantity)) {
            throw new InsufficientStockException(
                "Insufficient stock for product " + productId
            );
        }
        
        availableQuantity -= quantity;
        reservedQuantity += quantity;
        lastUpdatedAt = Instant.now();
        
        validateInvariant();
    }

    // Invariant validation
    private void validateInvariant() {
        int total = availableQuantity + reservedQuantity;
        if (total < 0) {
            throw new IllegalStateException("Invalid inventory state");
        }
    }
}
```

### 2. Value Object Pattern
```java
public class ReservationItem {
    private final UUID productId;
    private final int quantity;

    public ReservationItem(UUID productId, int quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        this.productId = productId;
        this.quantity = quantity;
    }

    @Override
    public boolean equals(Object o) {
        // Value equality
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, quantity);
    }

    // No setters (immutable)
    public UUID getProductId() { return productId; }
    public int getQuantity() { return quantity; }
}
```

### 3. Domain Service Pattern
```java
@Component  // ONLY annotation allowed
public class StockService {
    
    public void validateStockReservation(
            List<ReservationItem> items,
            Map<UUID, InventoryItem> inventory) {
        
        for (ReservationItem item : items) {
            InventoryItem inventoryItem = inventory.get(item.getProductId());
            
            if (inventoryItem == null) {
                throw new ProductNotFoundException(
                    "Product not found: " + item.getProductId()
                );
            }
            
            if (!inventoryItem.isAvailable(item.getQuantity())) {
                throw new InsufficientStockException(
                    "Insufficient stock for product: " + item.getProductId()
                );
            }
        }
    }

    public Instant calculateReservationExpiry(Instant currentTime) {
        return currentTime.plus(15, ChronoUnit.MINUTES);
    }
}
```

---

## Testing Strategy

### Unit Tests to Create

1. **InventoryItemTest.java**
   - Test `reserve()` success
   - Test `reserve()` insufficient stock
   - Test `release()` 
   - Test invariant validation
   - Test negative quantity rejection

2. **ReservationTest.java**
   - Test `isExpired()` logic
   - Test `confirm()` success
   - Test `confirm()` on expired (should fail)
   - Test `cancel()` success
   - Test `cancel()` on confirmed (should fail)

3. **StockServiceTest.java**
   - Test `validateStockReservation()` success
   - Test `validateStockReservation()` with insufficient stock
   - Test `calculateReservationExpiry()`

4. **ReservationServiceTest.java**
   - Test `createReservation()`
   - Test state transition validations

**Requirements:**
- Pure unit tests (NO Spring context)
- NO mocking needed (except for time)
- Fast execution (< 1 second total)

---

## File Breakdown

| File | Type | Lines Est. | Complexity |
|------|------|------------|------------|
| InventoryItem.java | Model | 120 | Medium |
| Reservation.java | Model | 100 | Medium |
| ReservationItem.java | Value Object | 40 | Low |
| ReservationStatus.java | Enum | 20 | Low |
| StockService.java | Service | 80 | Medium |
| ReservationService.java | Service | 60 | Low |
| InventoryRepository.java | Port | 20 | Low |
| ReservationRepository.java | Port | 20 | Low |
| InvalidReservationStateException.java | Exception | 10 | Low |

**Total:** ~470 lines

---

## Validation Criteria

- [ ] All domain classes created
- [ ] NO JPA annotations in domain layer
- [ ] All business logic in domain (not in services)
- [ ] `Instant` used for all timestamps
- [ ] UUID used for all IDs
- [ ] Domain services are stateless
- [ ] Port interfaces defined
- [ ] Exceptions moved to domain
- [ ] Unit tests passing (if created)
- [ ] Code compiles without Spring context

---

## Common Mistakes to Avoid

1. ❌ Adding `@Entity` to domain models
2. ❌ Using `LocalDateTime` instead of `Instant`
3. ❌ Putting repository calls in domain services
4. ❌ Making domain depend on infrastructure
5. ❌ Creating anemic domain models (getters/setters only)
6. ❌ Skipping invariant validation
7. ❌ Using mutable collections in domain

---

## Next Task

**Task 03:** Inventory Service - Application Layer Creation

---

**Status:** 🔄 READY TO START (AI Agent: Execute after Task 01 completion and validation)

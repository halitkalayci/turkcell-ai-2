# Task 03: Inventory Service - Application Layer Creation

**Task ID:** REFACTOR-03  
**Service:** inventory-service  
**Phase:** Implementation - Application Layer  
**Estimated Time:** 5 hours  
**Dependencies:** Task 02 (Domain Layer) ✅  

---

## Objective

Create application layer that orchestrates use-cases, manages transactions, and provides clear entry points for the system. This layer must be unit-testable without Spring context.

---

## Application Layer Principles

Per AGENTS.md Section 3.2:
- Orchestrates use-cases
- Transaction management
- Calls domain services via ports
- MUST BE unit-testable
- NO business logic here (only coordination)

---

## Files to Create

### Package: `application.usecase`

Use-cases represent the application's operations exposed to the outside world. Each use-case = one operation.

---

#### 1. `GetInventoryItemUseCase.java`
**Path:** `com.ecommerce.inventoryservice.application.usecase.GetInventoryItemUseCase`

**Purpose:** Retrieve inventory information for a product

```java
public class GetInventoryItemUseCase {
    private final InventoryRepository inventoryRepository;

    public InventoryItem execute(UUID productId) {
        return inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new ProductNotFoundException(
                "Product not found: " + productId));
    }
}
```

**Characteristics:**
- Simple read operation
- No transaction needed (read-only)
- Direct port usage
- Delegates exception handling to domain

---

#### 2. `CheckAvailabilityUseCase.java`
**Path:** `com.ecommerce.inventoryservice.application.usecase.CheckAvailabilityUseCase`

**Purpose:** Check if multiple products have sufficient stock

```java
public class CheckAvailabilityUseCase {
    private final InventoryRepository inventoryRepository;
    private final StockService stockService;

    public AvailabilityResult execute(List<ProductQuantity> items) {
        // 1. Load all inventory items
        List<UUID> productIds = extractProductIds(items);
        List<InventoryItem> inventoryItems = 
            inventoryRepository.findByProductIds(productIds);
        
        // 2. Validate all products exist
        validateAllProductsExist(productIds, inventoryItems);
        
        // 3. Check availability for each item
        Map<UUID, InventoryItem> inventoryMap = toMap(inventoryItems);
        Map<UUID, Integer> requestedQuantities = toQuantityMap(items);
        
        boolean allAvailable = stockService.checkMultipleAvailability(
            requestedQuantities, 
            inventoryMap
        );
        
        return new AvailabilityResult(allAvailable, 
                                      buildItemResults(items, inventoryMap));
    }
}
```

**Note:** This use-case coordinates but doesn't contain business logic.

---

#### 3. `CreateReservationUseCase.java`
**Path:** `com.ecommerce.inventoryservice.application.usecase.CreateReservationUseCase`

**Purpose:** Create stock reservation for an order (COMPLEX ORCHESTRATION)

```java
@Transactional  // ONLY place for @Transactional
public class CreateReservationUseCase {
    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final StockService stockService;
    private final ReservationService reservationService;

    public Reservation execute(UUID orderId, List<ReservationItem> items) {
        // 1. Validate no duplicate reservation
        reservationRepository.findByOrderId(orderId).ifPresent(existing -> {
            throw new ReservationConflictException(
                "Reservation already exists for order: " + orderId);
        });
        
        // 2. Load inventory items
        List<UUID> productIds = items.stream()
            .map(ReservationItem::getProductId)
            .toList();
        List<InventoryItem> inventoryItems = 
            inventoryRepository.findByProductIds(productIds);
        Map<UUID, InventoryItem> inventoryMap = toMap(inventoryItems);
        
        // 3. Validate stock availability (domain service)
        stockService.validateStockReservation(items, inventoryMap);
        
        // 4. Reserve stock in each inventory item (domain logic)
        for (ReservationItem item : items) {
            InventoryItem inventoryItem = inventoryMap.get(item.getProductId());
            inventoryItem.reserve(item.getQuantity());
        }
        
        // 5. Save updated inventory items
        inventoryRepository.saveAll(new ArrayList<>(inventoryMap.values()));
        
        // 6. Create reservation domain object
        Reservation reservation = reservationService.createReservation(orderId, items);
        
        // 7. Persist reservation
        return reservationRepository.save(reservation);
    }
}
```

**Characteristics:**
- Transactional boundary here
- Orchestrates multiple domain operations
- NO business rules (delegates to domain)
- Handles concurrency via optimistic locking

---

#### 4. `GetReservationUseCase.java`
**Path:** `com.ecommerce.inventoryservice.application.usecase.GetReservationUseCase`

```java
public class GetReservationUseCase {
    private final ReservationRepository reservationRepository;

    public Reservation execute(UUID reservationId) {
        return reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ReservationNotFoundException(
                "Reservation not found: " + reservationId));
    }
}
```

---

#### 5. `ConfirmReservationUseCase.java`
**Path:** `com.ecommerce.inventoryservice.application.usecase.ConfirmReservationUseCase`

**Purpose:** Confirm a reservation (order paid)

```java
@Transactional
public class ConfirmReservationUseCase {
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    public Reservation execute(UUID reservationId) {
        // 1. Load reservation
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ReservationNotFoundException(
                "Reservation not found: " + reservationId));
        
        // 2. Validate can be confirmed (domain logic)
        reservationService.validateReservationConfirmation(reservation);
        
        // 3. Confirm (domain method)
        reservation.confirm();
        
        // 4. Persist
        return reservationRepository.save(reservation);
    }
}
```

---

#### 6. `CancelReservationUseCase.java`
**Path:** `com.ecommerce.inventoryservice.application.usecase.CancelReservationUseCase`

**Purpose:** Cancel a reservation and release stock

```java
@Transactional
public class CancelReservationUseCase {
    private final ReservationRepository reservationRepository;
    private final InventoryRepository inventoryRepository;
    private final ReservationService reservationService;

    public Reservation execute(UUID reservationId) {
        // 1. Load reservation
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ReservationNotFoundException(
                "Reservation not found: " + reservationId));
        
        // 2. Validate can be cancelled (domain logic)
        reservationService.validateReservationCancellation(reservation);
        
        // 3. Load inventory items
        List<UUID> productIds = reservation.getItems().stream()
            .map(ReservationItem::getProductId)
            .toList();
        List<InventoryItem> inventoryItems = 
            inventoryRepository.findByProductIds(productIds);
        Map<UUID, InventoryItem> inventoryMap = toMap(inventoryItems);
        
        // 4. Release reserved quantities (domain logic)
        for (ReservationItem item : reservation.getItems()) {
            InventoryItem inventoryItem = inventoryMap.get(item.getProductId());
            inventoryItem.release(item.getQuantity());
        }
        
        // 5. Save inventory changes
        inventoryRepository.saveAll(new ArrayList<>(inventoryMap.values()));
        
        // 6. Cancel reservation (domain method)
        reservation.cancel();
        
        // 7. Persist
        return reservationRepository.save(reservation);
    }
}
```

---

### Package: `application.port.in`

Input ports = interfaces for use-cases (optional but recommended for clean architecture)

#### 7. `GetInventoryItemPort.java`
```java
public interface GetInventoryItemPort {
    InventoryItem execute(UUID productId);
}
```

*(Repeat for all use-cases if following strict hexagonal pattern)*

**Decision:** For simplicity (AGENTS.md Line 124), we may SKIP input port interfaces and use use-case classes directly. Controller will depend on concrete use-case classes.

---

### Supporting Classes

#### 8. `ProductQuantity.java`
**Path:** `com.ecommerce.inventoryservice.application.dto.ProductQuantity`

**Purpose:** Simple DTO for use-case input

```java
public record ProductQuantity(UUID productId, int quantity) {
    public ProductQuantity {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
}
```

---

#### 9. `AvailabilityResult.java`
**Path:** `com.ecommerce.inventoryservice.application.dto.AvailabilityResult`

```java
public record AvailabilityResult(
    boolean allAvailable,
    List<ItemAvailability> items
) {}

public record ItemAvailability(
    UUID productId,
    boolean available,
    int requestedQuantity,
    int availableQuantity
) {}
```

---

## Configuration

### Package: `config`

#### 10. `UseCaseConfig.java`
**Path:** `com.ecommerce.inventoryservice.config.UseCaseConfig`

**Purpose:** Spring bean configuration for use-cases

```java
@Configuration
public class UseCaseConfig {

    @Bean
    public GetInventoryItemUseCase getInventoryItemUseCase(
            InventoryRepository inventoryRepository) {
        return new GetInventoryItemUseCase(inventoryRepository);
    }

    @Bean
    public CheckAvailabilityUseCase checkAvailabilityUseCase(
            InventoryRepository inventoryRepository,
            StockService stockService) {
        return new CheckAvailabilityUseCase(inventoryRepository, stockService);
    }

    @Bean
    public CreateReservationUseCase createReservationUseCase(
            InventoryRepository inventoryRepository,
            ReservationRepository reservationRepository,
            StockService stockService,
            ReservationService reservationService) {
        return new CreateReservationUseCase(
            inventoryRepository,
            reservationRepository,
            stockService,
            reservationService
        );
    }

    // ... other use-cases
}
```

**Why manual configuration?**
- Use-cases are NOT annotated with `@Service` (they're not Spring components)
- Follows pure hexagonal architecture
- Better testability (can instantiate without Spring)

---

## Testing Strategy

### Unit Tests (NO Spring Context)

#### `CreateReservationUseCaseTest.java`
```java
class CreateReservationUseCaseTest {
    
    private InventoryRepository inventoryRepository;
    private ReservationRepository reservationRepository;
    private StockService stockService;
    private ReservationService reservationService;
    private CreateReservationUseCase useCase;

    @BeforeEach
    void setUp() {
        // Use MOCKS (Mockito) for repositories
        inventoryRepository = mock(InventoryRepository.class);
        reservationRepository = mock(ReservationRepository.class);
        
        // Use REAL domain services
        stockService = new StockService();
        reservationService = new ReservationService();
        
        useCase = new CreateReservationUseCase(
            inventoryRepository,
            reservationRepository,
            stockService,
            reservationService
        );
    }

    @Test
    void shouldCreateReservationSuccessfully() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ReservationItem item = new ReservationItem(productId, 5);
        
        InventoryItem inventoryItem = new InventoryItem(
            productId, 10, 0, 0L
        );
        
        when(reservationRepository.findByOrderId(orderId))
            .thenReturn(Optional.empty());
        when(inventoryRepository.findByProductIds(any()))
            .thenReturn(List.of(inventoryItem));
        when(reservationRepository.save(any()))
            .thenAnswer(inv -> inv.getArgument(0));
        
        // When
        Reservation result = useCase.execute(orderId, List.of(item));
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        verify(inventoryRepository).saveAll(any());
    }

    @Test
    void shouldThrowExceptionWhenInsufficientStock() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ReservationItem item = new ReservationItem(productId, 50); // Too much
        
        InventoryItem inventoryItem = new InventoryItem(
            productId, 10, 0, 0L  // Only 10 available
        );
        
        when(reservationRepository.findByOrderId(orderId))
            .thenReturn(Optional.empty());
        when(inventoryRepository.findByProductIds(any()))
            .thenReturn(List.of(inventoryItem));
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(orderId, List.of(item)))
            .isInstanceOf(InsufficientStockException.class);
        
        verify(inventoryRepository, never()).saveAll(any());
    }
}
```

**Key Points:**
- Mock only infrastructure (repositories)
- Use real domain services
- Fast execution (no database)
- Tests orchestration logic

---

## File Breakdown

| File | Type | Lines Est. | Complexity |
|------|------|------------|------------|
| GetInventoryItemUseCase.java | Use Case | 25 | Low |
| CheckAvailabilityUseCase.java | Use Case | 60 | Medium |
| CreateReservationUseCase.java | Use Case | 80 | High |
| GetReservationUseCase.java | Use Case | 25 | Low |
| ConfirmReservationUseCase.java | Use Case | 40 | Medium |
| CancelReservationUseCase.java | Use Case | 70 | High |
| ProductQuantity.java | DTO | 15 | Low |
| AvailabilityResult.java | DTO | 20 | Low |
| UseCaseConfig.java | Config | 60 | Low |

**Total:** ~395 lines

---

## Transaction Management

### Rules:
1. `@Transactional` ONLY on use-case methods that write
2. Read-only operations: NO transaction needed
3. Place annotation at METHOD level (not class)
4. Use Spring's transaction management

### Transactional Use-Cases:
- ✅ `CreateReservationUseCase.execute()`
- ✅ `ConfirmReservationUseCase.execute()`
- ✅ `CancelReservationUseCase.execute()`

### Non-Transactional:
- ❌ `GetInventoryItemUseCase.execute()`
- ❌ `CheckAvailabilityUseCase.execute()`
- ❌ `GetReservationUseCase.execute()`

---

## Validation Criteria

- [ ] All use-case classes created
- [ ] Use-cases orchestrate, do NOT contain business logic
- [ ] `@Transactional` only on write operations
- [ ] Use-cases depend on domain ports (not infrastructure)
- [ ] Configuration class provides Spring beans
- [ ] Unit tests can run WITHOUT Spring context
- [ ] Clear separation: orchestration vs. business logic

---

## Common Mistakes to Avoid

1. ❌ Putting business rules in use-cases
2. ❌ Using `@Service` on use-case classes
3. ❌ Transaction on every method
4. ❌ Direct dependency on JPA repositories
5. ❌ Complex logic in use-case (move to domain)
6. ❌ Testing with Spring context (should be pure unit tests)

---

## Next Task

**Task 04:** Inventory Service - Infrastructure Layer Refactoring

---

**Status:** 🔄 READY TO START (AI Agent: Execute after Task 02 completion and validation)

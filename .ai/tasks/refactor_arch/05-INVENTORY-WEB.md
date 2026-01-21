# Task 05: Inventory Service - Web Layer Updates

**Task ID:** REFACTOR-05  
**Service:** inventory-service  
**Phase:** Implementation - Web Layer  
**Estimated Time:** 3 hours  
**Dependencies:** Task 02, 03, 04 ✅  

---

## Objective

Update controllers to work with application layer use-cases and create necessary DTOs. Ensure clean separation between web layer and domain/application layers.

---

## Package Restructuring

Move controller and DTOs to `web` package:

```
web/
├── controller/
│   └── InventoryController.java
└── dto/
    ├── request/
    │   ├── AvailabilityCheckRequest.java
    │   └── StockReservationRequest.java
    └── response/
        ├── AvailabilityCheckResponse.java
        ├── InventoryItemResponse.java
        └── StockReservationResponse.java
```

---

## Files to Update

### 1. Move DTOs
**Action:** Move all DTOs from `dto/*` to `web/dto/*`

- `dto/request/*` → `web/dto/request/*`
- `dto/response/*` → `web/dto/response/*`

**No code changes** - just package relocation.

---

### 2. Update `InventoryController.java`

**Path:** `web/controller/InventoryController.java`

**Changes:**
- Inject use-cases instead of `InventoryService`
- Map between web DTOs and application DTOs
- Keep all OpenAPI annotations

```java
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inventory Service", description = "Stock management and reservation operations")
public class InventoryController {

    // Inject use-cases
    private final GetInventoryItemUseCase getInventoryItemUseCase;
    private final CheckAvailabilityUseCase checkAvailabilityUseCase;
    private final CreateReservationUseCase createReservationUseCase;
    private final GetReservationUseCase getReservationUseCase;
    private final ConfirmReservationUseCase confirmReservationUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;

    @GetMapping("/inventory-items/{productId}")
    @Operation(
            summary = "Get stock information for a product",
            description = "Retrieves current available quantity and stock status"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock information retrieved"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<InventoryItemResponse> getInventoryItem(
            @Parameter(description = "Unique identifier of the product", required = true)
            @PathVariable UUID productId) {
        log.debug("GET /api/v1/inventory-items/{}", productId);

        // Execute use-case
        InventoryItem item = getInventoryItemUseCase.execute(productId);

        // Map domain to response DTO
        InventoryItemResponse response = mapToInventoryItemResponse(item);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/inventory-items/availability-check")
    @Operation(
            summary = "Check stock availability for multiple products",
            description = "Validates whether requested quantities are available"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Availability check completed"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AvailabilityCheckResponse> checkStockAvailability(
            @Valid @RequestBody AvailabilityCheckRequest request) {
        log.debug("POST /api/v1/inventory-items/availability-check with {} items", 
                  request.getItems().size());

        // Map web DTO to application DTO
        List<ProductQuantity> items = request.getItems().stream()
            .map(item -> new ProductQuantity(item.getProductId(), item.getQuantity()))
            .toList();

        // Execute use-case
        AvailabilityResult result = checkAvailabilityUseCase.execute(items);

        // Map application result to web response
        AvailabilityCheckResponse response = mapToAvailabilityCheckResponse(result);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/inventory-reservations")
    @Operation(
            summary = "Create a stock reservation",
            description = "Reserves stock for an order with expiration time"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reservation created"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Insufficient stock or conflict",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<StockReservationResponse> createStockReservation(
            @Valid @RequestBody StockReservationRequest request) {
        log.debug("POST /api/v1/inventory-reservations for order {}", request.getOrderId());

        // Map web DTO to domain value objects
        List<ReservationItem> items = request.getItems().stream()
            .map(item -> new ReservationItem(item.getProductId(), item.getQuantity()))
            .toList();

        // Execute use-case
        Reservation reservation = createReservationUseCase.execute(
            request.getOrderId(), 
            items
        );

        // Map domain to response DTO
        StockReservationResponse response = mapToReservationResponse(reservation);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/inventory-reservations/{reservationId}")
    @Operation(
            summary = "Get reservation details",
            description = "Retrieves information about a specific reservation"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation retrieved"),
            @ApiResponse(responseCode = "404", description = "Reservation not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<StockReservationResponse> getReservation(
            @Parameter(description = "Unique identifier of the reservation", required = true)
            @PathVariable UUID reservationId) {
        log.debug("GET /api/v1/inventory-reservations/{}", reservationId);

        Reservation reservation = getReservationUseCase.execute(reservationId);
        StockReservationResponse response = mapToReservationResponse(reservation);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/inventory-reservations/{reservationId}/confirm")
    @Operation(
            summary = "Confirm a reservation",
            description = "Marks a reservation as confirmed (order paid)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation confirmed"),
            @ApiResponse(responseCode = "404", description = "Reservation not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Cannot confirm (expired or invalid state)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<StockReservationResponse> confirmReservation(
            @Parameter(description = "Unique identifier of the reservation", required = true)
            @PathVariable UUID reservationId) {
        log.debug("POST /api/v1/inventory-reservations/{}/confirm", reservationId);

        Reservation reservation = confirmReservationUseCase.execute(reservationId);
        StockReservationResponse response = mapToReservationResponse(reservation);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/inventory-reservations/{reservationId}/cancel")
    @Operation(
            summary = "Cancel a reservation",
            description = "Cancels a reservation and releases reserved stock"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation cancelled"),
            @ApiResponse(responseCode = "404", description = "Reservation not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Cannot cancel (already confirmed)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<StockReservationResponse> cancelReservation(
            @Parameter(description = "Unique identifier of the reservation", required = true)
            @PathVariable UUID reservationId) {
        log.debug("POST /api/v1/inventory-reservations/{}/cancel", reservationId);

        Reservation reservation = cancelReservationUseCase.execute(reservationId);
        StockReservationResponse response = mapToReservationResponse(reservation);

        return ResponseEntity.ok(response);
    }

    // Mapping methods
    private InventoryItemResponse mapToInventoryItemResponse(InventoryItem item) {
        return InventoryItemResponse.builder()
            .productId(item.getProductId())
            .availableQuantity(item.getAvailableQuantity())
            .reservedQuantity(item.getReservedQuantity())
            .totalQuantity(item.getTotalQuantity())
            .lastUpdatedAt(item.getLastUpdatedAt())
            .build();
    }

    private AvailabilityCheckResponse mapToAvailabilityCheckResponse(
            AvailabilityResult result) {
        // Map application DTO to web DTO
        List<AvailabilityCheckResponse.ItemAvailability> items = 
            result.items().stream()
                .map(item -> new AvailabilityCheckResponse.ItemAvailability(
                    item.productId(),
                    item.available(),
                    item.requestedQuantity(),
                    item.availableQuantity()
                ))
                .toList();

        return new AvailabilityCheckResponse(result.allAvailable(), items);
    }

    private StockReservationResponse mapToReservationResponse(Reservation reservation) {
        List<StockReservationResponse.ReservedItem> items = 
            reservation.getItems().stream()
                .map(item -> new StockReservationResponse.ReservedItem(
                    item.getProductId(),
                    item.getQuantity()
                ))
                .toList();

        return StockReservationResponse.builder()
            .reservationId(reservation.getId())
            .orderId(reservation.getOrderId())
            .items(items)
            .status(reservation.getStatus().name())
            .createdAt(reservation.getCreatedAt())
            .expiresAt(reservation.getExpiresAt())
            .build();
    }
}
```

**Key Changes:**
- Inject 6 use-cases (not service interface)
- Mapping logic in private methods
- Use-case execution is straightforward
- Domain objects → response DTOs

---

### 3. Update Response DTOs

#### `InventoryItemResponse.java`
**Change:** `LocalDateTime` → `Instant`

```java
@Builder
public record InventoryItemResponse(
    UUID productId,
    Integer availableQuantity,
    Integer reservedQuantity,
    Integer totalQuantity,
    Instant lastUpdatedAt  // Changed from LocalDateTime
) {}
```

---

#### `StockReservationResponse.java`
**Change:** `LocalDateTime` → `Instant`

```java
@Builder
public record StockReservationResponse(
    UUID reservationId,
    UUID orderId,
    List<ReservedItem> items,
    String status,
    Instant createdAt,    // Changed from LocalDateTime
    Instant expiresAt     // Changed from LocalDateTime
) {
    public record ReservedItem(
        UUID productId,
        Integer quantity
    ) {}
}
```

---

### 4. Update GlobalExceptionHandler

**Path:** Keep in `exception/GlobalExceptionHandler.java`

**Add new exceptions:**
```java
@ExceptionHandler(ReservationNotFoundException.class)
public ProblemDetail handleReservationNotFound(ReservationNotFoundException ex) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND,
        ex.getMessage()
    );
    problemDetail.setTitle("Reservation Not Found");
    return problemDetail;
}

@ExceptionHandler(InvalidReservationStateException.class)
public ProblemDetail handleInvalidReservationState(InvalidReservationStateException ex) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        ex.getMessage()
    );
    problemDetail.setTitle("Invalid Reservation State");
    return problemDetail;
}
```

---

## Integration Points

### Controller Dependencies

```java
@RequiredArgsConstructor
public class InventoryController {
    // 6 use-case dependencies
    private final GetInventoryItemUseCase getInventoryItemUseCase;
    private final CheckAvailabilityUseCase checkAvailabilityUseCase;
    private final CreateReservationUseCase createReservationUseCase;
    private final GetReservationUseCase getReservationUseCase;
    private final ConfirmReservationUseCase confirmReservationUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;
}
```

**Spring will auto-wire** these from `UseCaseConfig` beans.

---

## Testing Strategy

### Controller Integration Tests

Keep existing tests, update to work with new architecture:

```java
@SpringBootTest
@AutoConfigureMockMvc
class InventoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void shouldGetInventoryItem() throws Exception {
        // Given
        UUID productId = UUID.randomUUID();
        InventoryItem item = new InventoryItem(productId, 100, 0, 0L);
        inventoryRepository.save(item);

        // When/Then
        mockMvc.perform(get("/api/v1/inventory-items/{productId}", productId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productId").value(productId.toString()))
            .andExpect(jsonPath("$.availableQuantity").value(100));
    }

    @Test
    void shouldCreateReservation() throws Exception {
        // Given
        UUID productId = UUID.randomUUID();
        InventoryItem item = new InventoryItem(productId, 50, 0, 0L);
        inventoryRepository.save(item);

        StockReservationRequest request = new StockReservationRequest(
            UUID.randomUUID(),
            List.of(new StockReservationRequest.ItemRequest(productId, 10))
        );

        // When/Then
        mockMvc.perform(post("/api/v1/inventory-reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
```

---

## File Breakdown

| File | Action | Lines Changed |
|------|--------|---------------|
| InventoryController.java | UPDATE | ~250 lines (restructure) |
| InventoryItemResponse.java | UPDATE | 5 lines (Instant type) |
| StockReservationResponse.java | UPDATE | 10 lines (Instant type) |
| GlobalExceptionHandler.java | UPDATE | +20 lines (new handlers) |
| All DTOs | MOVE | 0 changes (package only) |

---

## Validation Criteria

- [ ] Controller uses use-cases (not service)
- [ ] All DTOs use `Instant` for timestamps
- [ ] Mapping logic in controller private methods
- [ ] All endpoints work (integration tests pass)
- [ ] OpenAPI documentation still valid
- [ ] Exception handling updated
- [ ] No direct domain object exposure in responses

---

## API Contract Compliance

**CRITICAL:** Ensure NO breaking changes to API contract

| Endpoint | Before | After | Status |
|----------|--------|-------|--------|
| GET /api/v1/inventory-items/{id} | ✅ | ✅ | Same |
| POST /api/v1/inventory-items/availability-check | ✅ | ✅ | Same |
| POST /api/v1/inventory-reservations | ✅ | ✅ | Same |
| GET /api/v1/inventory-reservations/{id} | ✅ | ✅ | Same |
| POST /api/v1/inventory-reservations/{id}/confirm | ✅ | ✅ | Same |
| POST /api/v1/inventory-reservations/{id}/cancel | ✅ | ✅ | Same |

**Note:** Response format stays identical (DTOs unchanged except `Instant` serialization).

---

## Common Mistakes to Avoid

1. ❌ Returning domain objects directly from controller
2. ❌ Putting mapping logic in use-cases
3. ❌ Breaking API contract with response changes
4. ❌ Forgetting to update GlobalExceptionHandler
5. ❌ Using `LocalDateTime` in response DTOs

---

## Next Task

**Task 06:** Order Service - Analysis

---

**Status:** 🔄 READY TO START (AI Agent: Execute after Task 04 completion and validation)

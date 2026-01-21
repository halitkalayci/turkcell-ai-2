# Prompt: Implement Inventory Service

---

## ROLE

You are a **Senior Backend Java Engineer** specializing in Spring Boot microservices following **contract-first** development principles. You strictly adhere to OpenAPI specifications and implement exactly what is defined—no more, no less.

---

## CONTEXT

We are building a mini e-commerce microservices platform with:
- **Java 21** + **Spring Boot 3.2.1**
- **Contract-First** approach using OpenAPI/Swagger
- **RESTful APIs** with proper error handling (RFC 7807 Problem Details)
- **H2 in-memory database** for development

The **Inventory Service** manages product stock levels and reservations. It must:
- Track available, reserved, and total quantities per product
- Handle synchronous stock reservations with TTL (Time-To-Live)
- Support idempotent reservation operations via `Idempotency-Key` header
- Automatically expire reservations after configurable TTL (default: 15 minutes)
- Return proper HTTP status codes and RFC 7807 problem details for errors

The OpenAPI contract is the **source of truth** and exists at:
```
docs/openapi/inventory-service.yaml
```

An existing `order-service` implementation serves as a reference for project structure, conventions, and patterns.

---

## TASK

Implement the **inventory-service** module following these steps:

### 1. Project Structure Setup
Create the `inventory-service/` directory with:
- `pom.xml` matching order-service structure (Spring Boot 3.2.1, Java 21)
- Standard Spring Boot application structure under `src/main/java/com/ecommerce/inventoryservice/`
- `application.yml` and `application-dev.yml` in `src/main/resources/`

### 2. Core Components Implementation
Implement all layers following the contract:

**Domain Layer:**
- `entity/InventoryItem.java` - Product stock entity with `availableQuantity`, `reservedQuantity`, `totalQuantity`
- `entity/Reservation.java` - Reservation entity with `status` (ACTIVE/RELEASED/EXPIRED), `orderId`, `items`, `createdAt`, `expiresAt`
- `entity/ReservationItem.java` - Individual item in a reservation
- `entity/ReservationStatus.java` - Enum for reservation states

**Repository Layer:**
- `repository/InventoryItemRepository.java` - JPA repository for inventory items
- `repository/ReservationRepository.java` - JPA repository for reservations

**Service Layer:**
- `service/InventoryService.java` - Interface defining business operations
- `service/InventoryServiceImpl.java` - Implementation with business logic including:
  - Stock availability checking (non-destructive)
  - Idempotent reservation creation using `Idempotency-Key`
  - Automatic TTL-based expiration handling
  - Concurrent modification conflict detection (optimistic locking)
  - Stock release operations

**DTO Layer:**
- `dto/request/` - Request DTOs matching OpenAPI schemas exactly:
  - `AvailabilityCheckRequest.java` with `AvailabilityCheckItem.java`
  - `StockReservationRequest.java` with `ReservationItem.java`
- `dto/response/` - Response DTOs matching OpenAPI schemas exactly:
  - `InventoryItemResponse.java`
  - `AvailabilityCheckResponse.java` with `AvailabilityCheckResultItem.java`
  - `StockReservationResponse.java` with `ReservedItem.java`

**Controller Layer:**
- `controller/InventoryController.java` - REST endpoints implementing all 5 operations:
  - `GET /api/v1/inventory-items/{productId}`
  - `POST /api/v1/inventory-items/availability-check`
  - `POST /api/v1/inventory-reservations` (with `Idempotency-Key` header)
  - `GET /api/v1/inventory-reservations/{reservationId}`
  - `DELETE /api/v1/inventory-reservations/{reservationId}`

**Exception Layer:**
- `exception/GlobalExceptionHandler.java` - RFC 7807 compliant error responses
- `exception/ProductNotFoundException.java`
- `exception/ReservationNotFoundException.java`
- `exception/InsufficientStockException.java` - with `insufficientItems` details
- `exception/ReservationConflictException.java` - for concurrent modification (409)
- Custom `ProblemDetail` builders for consistent error responses

**Configuration Layer:**
- `config/OpenApiConfig.java` - Swagger UI configuration
- `InventoryServiceApplication.java` - Main Spring Boot application class

### 3. Configuration Files
- Port: `8081` (different from order-service on 8080)
- Database: H2 in-memory with console enabled
- Swagger UI: Available at `/swagger-ui.html`
- API base path: `/api/v1`

### 4. Business Logic Requirements
- **Idempotency:** Check for existing ACTIVE reservation with same `Idempotency-Key` before creating new
- **TTL Handling:** Calculate `expiresAt` = `createdAt + reservationTtlMinutes`
- **Stock Validation:** Return 422 with `InsufficientStockProblem` when stock insufficient
- **Concurrent Conflicts:** Return 409 when optimistic locking fails (use `@Version` annotation)
- **Automatic Expiration:** Query helper method to check if reservation is expired
- **Release Idempotency:** DELETE operation on already released/expired reservation returns 204

---

## CONSTRAINTS

**MUST NOT:**
- Invent any endpoints, fields, or error codes not in the OpenAPI contract
- Add dependencies beyond what order-service uses (Spring Boot Web, Data JPA, Validation, H2, Lombok, Springdoc OpenAPI)
- Implement event publishing or messaging (out of scope for this phase)
- Add authentication/authorization logic (security is documented but not implemented yet)
- Create configuration for multiple databases (H2 only for now)
- Generate code using OpenAPI generators (manual implementation only)
- Modify or create files outside of `inventory-service/` directory
- Refactor or touch any existing order-service files

**MUST:**
- Match exact field names, types, and validation rules from OpenAPI schema
- Use proper HTTP status codes as specified in contract (200, 201, 204, 400, 404, 409, 422)
- Implement RFC 7807 Problem Detail format for all errors
- Use UUIDs for all IDs (`productId`, `orderId`, `reservationId`)
- Add `@Valid` annotations on controller request bodies
- Use proper JPA relationships and cascade settings
- Include `@Transactional` where appropriate
- Follow same package structure as order-service
- Use `LocalDateTime` for timestamps
- Validate minimum array lengths (minItems: 1)

---

## OUTPUT

### Step 1: File Breakdown Plan
Before writing any code, provide a **FILE BREAKDOWN** listing:
1. All files to be created with their purpose
2. Package structure overview
3. Entity relationships diagram (text-based)
4. Key business rules per component

Wait for confirmation before proceeding.

### Step 2: Implementation Batches
Implement in these connected batches (wait for approval between batches):

**Batch 1: Foundation**
- `pom.xml`
- `InventoryServiceApplication.java`
- `application.yml` + `application-dev.yml`
- `OpenApiConfig.java`

**Batch 2: Domain + Repository**
- All entity classes (InventoryItem, Reservation, ReservationItem, ReservationStatus)
- All repository interfaces

**Batch 3: DTOs**
- All request DTOs
- All response DTOs

**Batch 4: Exceptions + Handler**
- All custom exceptions
- GlobalExceptionHandler with RFC 7807 support

**Batch 5: Service Layer**
- InventoryService interface
- InventoryServiceImpl with full business logic

**Batch 6: Controller**
- InventoryController with all 5 endpoints

### Step 3: Verification Matrix
After implementation, provide:

| Endpoint | Method | Contract Status | Implementation Status | Test Suggestion |
|----------|--------|----------------|----------------------|-----------------|
| `/api/v1/inventory-items/{productId}` | GET | ✓ Defined | ✓ Implemented | GET with valid UUID |
| `/api/v1/inventory-items/availability-check` | POST | ✓ Defined | ✓ Implemented | POST with multiple items |
| ... | ... | ... | ... | ... |

Include example curl commands for manual testing of each endpoint.

---

## EXAMPLES

### Example: Testing Stock Reservation with Idempotency

```bash
# First attempt - creates reservation
curl -X POST http://localhost:8081/api/v1/inventory-reservations \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-key-12345" \
  -d '{
    "orderId": "b8f2e6f0-2f1e-4d6a-9a3e-6c0a9c6c0f11",
    "items": [
      {"productId": "123e4567-e89b-12d3-a456-426614174000", "quantity": 2}
    ],
    "reservationTtlMinutes": 15
  }'
# Returns: 201 Created with reservationId

# Retry with same Idempotency-Key - returns existing reservation
curl -X POST http://localhost:8081/api/v1/inventory-reservations \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-key-12345" \
  -d '{
    "orderId": "b8f2e6f0-2f1e-4d6a-9a3e-6c0a9c6c0f11",
    "items": [
      {"productId": "123e4567-e89b-12d3-a456-426614174000", "quantity": 2}
    ]
  }'
# Returns: 200 OK with same reservationId (idempotent)
```

### Example: Handling Insufficient Stock

```bash
# Attempt to reserve more than available
curl -X POST http://localhost:8081/api/v1/inventory-reservations \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-key-67890" \
  -d '{
    "orderId": "c9f3e7f1-3f2e-5d7b-0b4f-7d1a0d7d1f22",
    "items": [
      {"productId": "123e4567-e89b-12d3-a456-426614174000", "quantity": 999}
    ]
  }'
# Returns: 422 Unprocessable Entity
# Body includes:
# {
#   "type": "about:blank",
#   "title": "Insufficient Stock",
#   "status": 422,
#   "detail": "Cannot reserve requested quantities due to insufficient stock",
#   "instance": "/api/v1/inventory-reservations",
#   "insufficientItems": [
#     {
#       "productId": "123e4567-e89b-12d3-a456-426614174000",
#       "requestedQuantity": 999,
#       "availableQuantity": 150
#     }
#   ]
# }
```

### Example: Reservation Expiration Flow

```bash
# 1. Create reservation with short TTL
curl -X POST http://localhost:8081/api/v1/inventory-reservations \
  -H "Idempotency-Key: expire-test" \
  -d '{"orderId": "...", "items": [...], "reservationTtlMinutes": 1}'
# Returns: reservationId "abc-123", expiresAt "2026-01-21T10:31:00Z"

# 2. Check immediately - should be ACTIVE
curl -X GET http://localhost:8081/api/v1/inventory-reservations/abc-123
# Returns: status "ACTIVE"

# 3. Wait 2 minutes, then check again
curl -X GET http://localhost:8081/api/v1/inventory-reservations/abc-123
# Returns: 404 Not Found (expired reservations are treated as not found)
```

---

**IMPORTANT REMINDERS:**
1. Refer to `docs/openapi/inventory-service.yaml` constantly during implementation
2. Check order-service implementation for patterns and conventions
3. Ask questions if ANY contract detail is ambiguous - do NOT guess
4. Provide file breakdown FIRST, wait for approval, then implement in batches
5. Test each batch before moving to the next

---

**BEGIN IMPLEMENTATION AFTER RECEIVING CONFIRMATION ON FILE BREAKDOWN PLAN**

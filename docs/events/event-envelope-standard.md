# Event Envelope Standard

## Mandatory Structure
ALL events MUST follow this JSON structure:

```json
{
  "eventId": "uuid",
  "eventType": "string",
  "eventTimestamp": "ISO-8601",
  "correlationId": "uuid",
  "aggregateId": "uuid",
  "payload": {
    // Event-specific data
  }
}
```

## Field Descriptions

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| eventId | UUID | Yes | Unique identifier for this event (idempotency key) |
| eventType | String | Yes | Event type (e.g., "OrderCreated") |
| eventTimestamp | ISO-8601 | Yes | When event was created (e.g., "2026-01-22T10:30:00Z") |
| correlationId | UUID | Yes | Trace ID for distributed tracing across services |
| aggregateId | UUID | Yes | Business entity ID (orderId, productId, etc.) |
| payload | Object | Yes | Event-specific data (varies by event type) |

## Data Type Rules

### Money
- **Type:** String representation of BigDecimal
- **Format:** Decimal notation with two decimal places
- **Examples:** "100.50", "1005.00", "0.99"
- **Why:** Avoids floating-point precision issues
- **Serialization:** BigDecimal → String in JSON

### Timestamps
- **Type:** String in ISO-8601 format
- **Format:** `yyyy-MM-dd'T'HH:mm:ss'Z'` (UTC)
- **Examples:** "2026-01-22T10:30:00Z", "2026-01-22T14:45:30.123Z"
- **Java Type:** `Instant` or `OffsetDateTime`

### IDs
- **Type:** UUID (version 4)
- **Format:** 36-character string with hyphens
- **Example:** "550e8400-e29b-41d4-a716-446655440000"
- **Java Type:** `java.util.UUID`

### Quantities
- **Type:** Integer
- **Examples:** 1, 10, 100
- **Java Type:** `int` or `Integer`

### Status/Enums
- **Type:** String (uppercase with underscores)
- **Examples:** "PENDING", "CONFIRMED", "CANCELLED"
- **Java Type:** `enum`

## Example Events

### OrderCreated Event
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "OrderCreated",
  "eventTimestamp": "2026-01-22T10:30:00Z",
  "correlationId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "aggregateId": "550e8400-e29b-41d4-a716-446655440001",
  "payload": {
    "orderId": "550e8400-e29b-41d4-a716-446655440001",
    "customerId": "123e4567-e89b-12d3-a456-426614174000",
    "items": [
      {
        "productId": "789e4567-e89b-12d3-a456-426614174000",
        "quantity": 10,
        "price": "100.50"
      }
    ],
    "totalAmount": "1005.00",
    "status": "PENDING"
  }
}
```

### ItemsReserved Event
```json
{
  "eventId": "6a7e8400-e29b-41d4-a716-446655440002",
  "eventType": "ItemsReserved",
  "eventTimestamp": "2026-01-22T10:30:05Z",
  "correlationId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "aggregateId": "9b8e8400-e29b-41d4-a716-446655440003",
  "payload": {
    "reservationId": "9b8e8400-e29b-41d4-a716-446655440003",
    "orderId": "550e8400-e29b-41d4-a716-446655440001",
    "items": [
      {
        "productId": "789e4567-e89b-12d3-a456-426614174000",
        "quantity": 10
      }
    ]
  }
}
```

## Java Implementation

### Base Event Class
```java
public abstract class BaseEvent {
    private UUID eventId;
    private String eventType;
    private Instant eventTimestamp;
    private UUID correlationId;
    private UUID aggregateId;

    // Constructor, getters, setters
}
```

### Concrete Event
```java
public class OrderCreatedEvent extends BaseEvent {
    private OrderCreatedPayload payload;

    public OrderCreatedEvent(UUID orderId, UUID customerId, List<OrderItem> items, BigDecimal totalAmount) {
        this.setEventId(UUID.randomUUID());
        this.setEventType("OrderCreated");
        this.setEventTimestamp(Instant.now());
        this.setCorrelationId(UUID.randomUUID()); // or from context
        this.setAggregateId(orderId);
        
        this.payload = new OrderCreatedPayload(orderId, customerId, items, totalAmount);
    }
}

@Data
class OrderCreatedPayload {
    private UUID orderId;
    private UUID customerId;
    private List<OrderItem> items;
    private String totalAmount; // BigDecimal serialized as String
    private String status;
}
```

### Jackson Serialization
```java
@Configuration
public class JacksonConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }
}
```

## Validation Rules

Before publishing an event:
1. ✅ All required fields are populated
2. ✅ `eventId` is unique (UUID.randomUUID())
3. ✅ `eventType` matches event class name
4. ✅ `eventTimestamp` is current time
5. ✅ `correlationId` is present (new or from context)
6. ✅ `aggregateId` matches business entity ID
7. ✅ `payload` contains all required business data

## Idempotency Key

The `eventId` field serves as the idempotency key:
- Consumers store processed `eventId` values
- Before processing, check if `eventId` already exists
- If exists, skip processing (already handled)
- This prevents duplicate processing of the same event

## Correlation Tracking

The `correlationId` field enables distributed tracing:
- Same `correlationId` across all events in a workflow
- Example: Order creation → Inventory reservation → Order confirmation
- All events share the same `correlationId`
- Useful for debugging and monitoring

## Event Immutability

Once an event is published:
- ❌ NEVER modify the event structure
- ❌ NEVER change field meanings
- ✅ Add new optional fields (backward compatible)
- ✅ Create new event type for breaking changes (e.g., OrderCreatedV2)

## Schema Evolution

### Backward Compatible Changes (Allowed)
- Adding new optional fields to payload
- Adding new event types
- Deprecating fields (but keeping them)

### Breaking Changes (Not Allowed)
- Removing fields
- Changing field types
- Changing field meanings
- Renaming fields

**Solution for Breaking Changes:** Create a new event type with a version suffix.

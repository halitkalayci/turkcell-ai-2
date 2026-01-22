# Inventory Service Events

## Overview
Inventory Service produces events related to inventory reservations and stock management.

---

## 1. ItemsReserved Event

**Destination:** `inventory-service.items.reserved`  
**Producer:** Inventory Service  
**Consumers:** Order Service, Notification Service

### Event Schema
```json
{
  "eventId": "uuid",
  "eventType": "ItemsReserved",
  "eventTimestamp": "ISO-8601",
  "correlationId": "uuid",
  "aggregateId": "reservationId",
  "payload": {
    "reservationId": "uuid",
    "orderId": "uuid",
    "items": [
      {
        "productId": "uuid",
        "quantity": 10,
        "reservedQuantity": 10
      }
    ]
  }
}
```

### When Published
- After successfully reserving items for an order
- Stock quantity decremented in database
- Reservation record created
- Via Transactional Outbox Pattern

### Consumer Actions
- **Order Service:** Update order status to CONFIRMED
- **Notification Service:** Send reservation success notification

### Idempotency Key
Use `eventId` from envelope

### Business Rules
- All items must be available in sufficient quantity
- Reservation is atomic - all items reserved or none
- Stock quantity must be >= requested quantity

---

## 2. ReservationFailed Event

**Destination:** `inventory-service.items.reservation-failed`  
**Producer:** Inventory Service  
**Consumers:** Order Service, Notification Service

### Event Schema
```json
{
  "eventId": "uuid",
  "eventType": "ReservationFailed",
  "eventTimestamp": "ISO-8601",
  "correlationId": "uuid",
  "aggregateId": "orderId",
  "payload": {
    "orderId": "uuid",
    "reason": "INSUFFICIENT_STOCK",
    "unavailableItems": [
      {
        "productId": "uuid",
        "requestedQuantity": 10,
        "availableQuantity": 5
      }
    ]
  }
}
```

### When Published
- When insufficient stock to fulfill order
- Before any database changes (read-only check)
- Via Transactional Outbox Pattern

### Failure Reasons
- `INSUFFICIENT_STOCK` - Not enough items in inventory
- `PRODUCT_NOT_FOUND` - Product does not exist
- `PRODUCT_DISCONTINUED` - Product no longer available

### Consumer Actions
- **Order Service:** Cancel order (update status to CANCELLED)
- **Notification Service:** Send stock unavailable notification

### Idempotency Key
Use `eventId` from envelope

---

## 3. ReservationReleased Event

**Destination:** `inventory-service.items.reservation-released`  
**Producer:** Inventory Service  
**Consumers:** Notification Service

### Event Schema
```json
{
  "eventId": "uuid",
  "eventType": "ReservationReleased",
  "eventTimestamp": "ISO-8601",
  "correlationId": "uuid",
  "aggregateId": "reservationId",
  "payload": {
    "reservationId": "uuid",
    "orderId": "uuid",
    "items": [
      {
        "productId": "uuid",
        "releasedQuantity": 10
      }
    ],
    "reason": "ORDER_CANCELLED"
  }
}
```

### When Published
- After receiving OrderCancelled event from Order Service
- Stock returned to available inventory
- Reservation marked as released
- Via Transactional Outbox Pattern

### Release Reasons
- `ORDER_CANCELLED` - Order was cancelled
- `ORDER_TIMEOUT` - Order expired (future)
- `MANUAL_RELEASE` - Admin released reservation

### Consumer Actions
- **Notification Service:** Log release event (no customer notification needed)

### Idempotency Key
Use `eventId` from envelope

---

## Spring Cloud Stream Configuration

### application.yml
```yaml
spring:
  cloud:
    function:
      definition: itemsReservedSupplier;reservationFailedSupplier;reservationReleasedSupplier
    stream:
      bindings:
        itemsReservedSupplier-out-0:
          destination: inventory-service.items.reserved
          content-type: application/json
        reservationFailedSupplier-out-0:
          destination: inventory-service.items.reservation-failed
          content-type: application/json
        reservationReleasedSupplier-out-0:
          destination: inventory-service.items.reservation-released
          content-type: application/json
      kafka:
        binder:
          brokers: localhost:29092
```

### Producer Implementation
```java
@Configuration
public class InventoryEventProducers {
    
    @Bean
    public Supplier<Flux<Message<ItemsReservedEvent>>> itemsReservedSupplier(
            OutboxEventPublisher publisher) {
        return () -> publisher.pollAndPublish("ItemsReserved");
    }
    
    @Bean
    public Supplier<Flux<Message<ReservationFailedEvent>>> reservationFailedSupplier(
            OutboxEventPublisher publisher) {
        return () -> publisher.pollAndPublish("ReservationFailed");
    }
    
    @Bean
    public Supplier<Flux<Message<ReservationReleasedEvent>>> reservationReleasedSupplier(
            OutboxEventPublisher publisher) {
        return () -> publisher.pollAndPublish("ReservationReleased");
    }
}
```

### Consumer Implementation (OrderCreated)
```java
@Configuration
public class InventoryEventConsumers {
    
    private final ReservationService reservationService;
    
    @Bean
    public Consumer<Message<OrderCreatedEvent>> orderCreatedConsumer() {
        return message -> {
            OrderCreatedEvent event = message.getPayload();
            reservationService.handleOrderCreated(event);
        };
    }
    
    @Bean
    public Consumer<Message<OrderCancelledEvent>> orderCancelledConsumer() {
        return message -> {
            OrderCancelledEvent event = message.getPayload();
            reservationService.handleOrderCancelled(event);
        };
    }
}
```

---

## Event Flow Examples

### Happy Path: Successful Reservation
```
1. Inventory Service → Receive OrderCreated
2. Check stock availability → ALL ITEMS AVAILABLE
3. Decrement stock quantities
4. Create reservation record
5. Save ItemsReserved to outbox
6. Outbox Publisher → Publish ItemsReserved
7. Order Service → Receive ItemsReserved → Confirm Order
```

### Failure Path: Insufficient Stock
```
1. Inventory Service → Receive OrderCreated
2. Check stock availability → INSUFFICIENT STOCK
3. No database changes (read-only check)
4. Save ReservationFailed to outbox
5. Outbox Publisher → Publish ReservationFailed
6. Order Service → Receive ReservationFailed → Cancel Order
```

### Reservation Release
```
1. Inventory Service → Receive OrderCancelled
2. Find reservation by orderId
3. Increment stock quantities (return to inventory)
4. Mark reservation as released
5. Save ReservationReleased to outbox
6. Outbox Publisher → Publish ReservationReleased
```

---

## Database Schema

### Inventory Items Table
```sql
CREATE TABLE inventory_items (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL UNIQUE,
    available_quantity INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL,
    total_quantity INTEGER NOT NULL,
    version INTEGER NOT NULL, -- Optimistic locking
    CONSTRAINT check_quantities CHECK (available_quantity >= 0 AND reserved_quantity >= 0)
);
```

### Reservations Table
```sql
CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL, -- ACTIVE, RELEASED
    created_at TIMESTAMP NOT NULL,
    released_at TIMESTAMP,
    version INTEGER NOT NULL
);

CREATE TABLE reservation_items (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    FOREIGN KEY (reservation_id) REFERENCES reservations(id)
);
```

---

## Concurrency Handling

### Optimistic Locking
```java
@Entity
public class InventoryItem {
    @Id
    private UUID id;
    
    private UUID productId;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    
    @Version
    private Integer version; // JPA optimistic locking
}
```

### Reservation Logic
```java
@Service
@Transactional
public class ReservationService {
    
    public void reserveItems(OrderCreatedEvent event) {
        UUID eventId = event.getEventId();
        
        // Idempotency check
        if (processedEventRepository.existsByEventId(eventId)) {
            return;
        }
        
        try {
            // Check availability
            List<InventoryItem> items = checkAvailability(event.getPayload().getItems());
            
            if (!allItemsAvailable(items)) {
                publishReservationFailed(event);
                markAsProcessed(eventId);
                return;
            }
            
            // Decrement stock (with optimistic locking)
            items.forEach(item -> item.decrementAvailable(quantity));
            inventoryRepository.saveAll(items);
            
            // Create reservation
            Reservation reservation = createReservation(event);
            reservationRepository.save(reservation);
            
            // Publish success event
            publishItemsReserved(reservation);
            
            // Mark as processed
            markAsProcessed(eventId);
            
        } catch (OptimisticLockException e) {
            // Retry will happen via Spring Cloud Stream retry mechanism
            throw new ReservationConflictException("Concurrent modification", e);
        }
    }
}
```

---

## Testing

### Integration Test Example
```java
@SpringBootTest
@EmbeddedKafka
class InventoryEventsIntegrationTest {
    
    @Autowired
    private InputDestination input;
    
    @Autowired
    private OutputDestination output;
    
    @Test
    void shouldPublishItemsReservedWhenStockAvailable() {
        // Given: Stock available
        setupInventory(productId, 100);
        
        // When: Order created event received
        OrderCreatedEvent orderCreated = createOrderCreatedEvent(productId, 10);
        input.send(MessageBuilder.withPayload(orderCreated).build(), 
                   "order-service.orders.created");
        
        // Then: ItemsReserved published
        Message<byte[]> message = output.receive(5000, "inventory-service.items.reserved");
        assertThat(message).isNotNull();
        
        ItemsReservedEvent event = deserialize(message.getPayload());
        assertThat(event.getEventType()).isEqualTo("ItemsReserved");
    }
    
    @Test
    void shouldPublishReservationFailedWhenInsufficientStock() {
        // Given: Insufficient stock
        setupInventory(productId, 5);
        
        // When: Order created event received for 10 items
        OrderCreatedEvent orderCreated = createOrderCreatedEvent(productId, 10);
        input.send(MessageBuilder.withPayload(orderCreated).build(), 
                   "order-service.orders.created");
        
        // Then: ReservationFailed published
        Message<byte[]> message = output.receive(5000, "inventory-service.items.reservation-failed");
        assertThat(message).isNotNull();
        
        ReservationFailedEvent event = deserialize(message.getPayload());
        assertThat(event.getPayload().getReason()).isEqualTo("INSUFFICIENT_STOCK");
    }
}
```

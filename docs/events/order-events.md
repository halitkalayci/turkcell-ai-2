# Order Service Events

## Overview
Order Service produces events related to order lifecycle.

---

## 1. OrderCreated Event

**Destination:** `order-service.orders.created`  
**Producer:** Order Service  
**Consumers:** Inventory Service, Notification Service

### Event Schema
```json
{
  "eventId": "uuid",
  "eventType": "OrderCreated",
  "eventTimestamp": "ISO-8601",
  "correlationId": "uuid",
  "aggregateId": "orderId",
  "payload": {
    "orderId": "uuid",
    "customerId": "uuid",
    "items": [
      {
        "productId": "uuid",
        "quantity": 10,
        "price": "100.50"
      }
    ],
    "totalAmount": "1005.00",
    "status": "PENDING"
  }
}
```

### When Published
- After order is created in database with status PENDING
- Via Transactional Outbox Pattern
- Publisher polls outbox every 10 seconds

### Consumer Actions
- **Inventory Service:** Reserve items for order
- **Notification Service:** Send order confirmation email

### Idempotency Key
Use `eventId` from envelope

### Business Rules
- Order must have at least one item
- Total amount must equal sum of (quantity × price) for all items
- Status is always PENDING when created

---

## 2. OrderConfirmed Event

**Destination:** `order-service.orders.confirmed`  
**Producer:** Order Service  
**Consumers:** Notification Service

### Event Schema
```json
{
  "eventId": "uuid",
  "eventType": "OrderConfirmed",
  "eventTimestamp": "ISO-8601",
  "correlationId": "uuid",
  "aggregateId": "orderId",
  "payload": {
    "orderId": "uuid",
    "customerId": "uuid",
    "status": "CONFIRMED"
  }
}
```

### When Published
- After receiving ItemsReserved event from Inventory Service
- Order status updated to CONFIRMED in database
- Via Transactional Outbox Pattern

### Consumer Actions
- **Notification Service:** Send order confirmed notification

### Idempotency Key
Use `eventId` from envelope

---

## 3. OrderCancelled Event

**Destination:** `order-service.orders.cancelled`  
**Producer:** Order Service  
**Consumers:** Inventory Service, Notification Service

### Event Schema
```json
{
  "eventId": "uuid",
  "eventType": "OrderCancelled",
  "eventTimestamp": "ISO-8601",
  "correlationId": "uuid",
  "aggregateId": "orderId",
  "payload": {
    "orderId": "uuid",
    "customerId": "uuid",
    "reason": "CUSTOMER_REQUEST",
    "status": "CANCELLED"
  }
}
```

### When Published
- After receiving ReservationFailed event from Inventory Service
- Manual cancellation by customer or admin via REST API
- Via Transactional Outbox Pattern

### Cancellation Reasons
- `CUSTOMER_REQUEST` - Customer cancelled order
- `RESERVATION_FAILED` - Inventory reservation failed
- `ADMIN_CANCELLATION` - Admin cancelled order
- `PAYMENT_FAILED` - Payment processing failed (future)
- `TIMEOUT` - Order timeout (future)

### Consumer Actions
- **Inventory Service:** Release reservation (if any exists)
- **Notification Service:** Send cancellation notification

### Idempotency Key
Use `eventId` from envelope

---

## Spring Cloud Stream Configuration

### application.yml
```yaml
spring:
  cloud:
    function:
      definition: orderCreatedSupplier;orderConfirmedSupplier;orderCancelledSupplier
    stream:
      bindings:
        orderCreatedSupplier-out-0:
          destination: order-service.orders.created
          content-type: application/json
        orderConfirmedSupplier-out-0:
          destination: order-service.orders.confirmed
          content-type: application/json
        orderCancelledSupplier-out-0:
          destination: order-service.orders.cancelled
          content-type: application/json
      kafka:
        binder:
          brokers: localhost:29092
```

### Producer Implementation
```java
@Configuration
public class OrderEventProducers {
    
    @Bean
    public Supplier<Flux<Message<OrderCreatedEvent>>> orderCreatedSupplier(
            OutboxEventPublisher publisher) {
        return () -> publisher.pollAndPublish("OrderCreated");
    }
    
    @Bean
    public Supplier<Flux<Message<OrderConfirmedEvent>>> orderConfirmedSupplier(
            OutboxEventPublisher publisher) {
        return () -> publisher.pollAndPublish("OrderConfirmed");
    }
    
    @Bean
    public Supplier<Flux<Message<OrderCancelledEvent>>> orderCancelledSupplier(
            OutboxEventPublisher publisher) {
        return () -> publisher.pollAndPublish("OrderCancelled");
    }
}
```

---

## Event Flow Examples

### Happy Path: Order Creation to Confirmation
```
1. Order Service → Create Order (PENDING) → Save to outbox
2. Outbox Publisher → Poll outbox → Publish OrderCreated
3. Inventory Service → Receive OrderCreated → Reserve Items
4. Inventory Service → Publish ItemsReserved
5. Order Service → Receive ItemsReserved → Update Order (CONFIRMED)
6. Order Service → Publish OrderConfirmed
7. Notification Service → Receive OrderConfirmed → Send email
```

### Failure Path: Insufficient Stock
```
1. Order Service → Create Order (PENDING) → Save to outbox
2. Outbox Publisher → Poll outbox → Publish OrderCreated
3. Inventory Service → Receive OrderCreated → Check Stock → INSUFFICIENT
4. Inventory Service → Publish ReservationFailed
5. Order Service → Receive ReservationFailed → Update Order (CANCELLED)
6. Order Service → Publish OrderCancelled
7. Notification Service → Receive OrderCancelled → Send email
```

### Manual Cancellation
```
1. Customer/Admin → DELETE /api/v1/orders/{id}
2. Order Service → Update Order (CANCELLED) → Save to outbox
3. Outbox Publisher → Poll outbox → Publish OrderCancelled
4. Inventory Service → Receive OrderCancelled → Release Reservation
5. Notification Service → Receive OrderCancelled → Send email
```

---

## Testing

### Integration Test Example
```java
@SpringBootTest
@EmbeddedKafka
class OrderEventsIntegrationTest {
    
    @Autowired
    private OutputDestination output;
    
    @Test
    void shouldPublishOrderCreatedEvent() {
        // When: Create order
        orderService.createOrder(createOrderRequest());
        
        // Then: Event published
        Message<byte[]> message = output.receive(5000, "order-service.orders.created");
        assertThat(message).isNotNull();
        
        OrderCreatedEvent event = deserialize(message.getPayload());
        assertThat(event.getEventType()).isEqualTo("OrderCreated");
        assertThat(event.getPayload().getStatus()).isEqualTo("PENDING");
    }
}
```

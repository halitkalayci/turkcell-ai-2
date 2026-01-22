# Notification Service Events

## Overview
Notification Service is a **pure consumer** - it does NOT produce events.  
It listens to events from all services and sends notifications to customers.

---

## Consumed Events

### From Order Service

#### 1. OrderCreated
**Destination:** `order-service.orders.created`

**Action:** Send order confirmation email

**Email Template:**
```
Subject: Order Confirmation - #{orderId}

Dear Customer,

Your order has been received and is being processed.

Order ID: #{orderId}
Total Amount: #{totalAmount}
Status: Pending

We'll notify you once your order is confirmed.

Thank you for shopping with us!
```

---

#### 2. OrderConfirmed
**Destination:** `order-service.orders.confirmed`

**Action:** Send order success email

**Email Template:**
```
Subject: Order Confirmed - #{orderId}

Dear Customer,

Great news! Your order has been confirmed.

Order ID: #{orderId}
Status: Confirmed

Your items have been reserved and will be shipped soon.

Thank you!
```

---

#### 3. OrderCancelled
**Destination:** `order-service.orders.cancelled`

**Action:** Send cancellation email

**Email Template:**
```
Subject: Order Cancelled - #{orderId}

Dear Customer,

Your order has been cancelled.

Order ID: #{orderId}
Reason: #{reason}
Status: Cancelled

If you have any questions, please contact our support team.

Sorry for the inconvenience.
```

---

### From Inventory Service

#### 1. ItemsReserved
**Destination:** `inventory-service.items.reserved`

**Action:** Send reservation success notification (optional)

**Email Template:**
```
Subject: Items Reserved - Order #{orderId}

Dear Customer,

Good news! All items in your order have been reserved.

Order ID: #{orderId}
Reservation ID: #{reservationId}

Your order will be processed shortly.

Thank you!
```

---

#### 2. ReservationFailed
**Destination:** `inventory-service.items.reservation-failed`

**Action:** Send stock unavailable notification

**Email Template:**
```
Subject: Order Cannot Be Fulfilled - #{orderId}

Dear Customer,

Unfortunately, we cannot fulfill your order due to insufficient stock.

Order ID: #{orderId}
Reason: #{reason}

Unavailable Items:
#{unavailableItemsList}

Your order has been cancelled and no charges have been made.

We apologize for the inconvenience.
```

---

#### 3. ReservationReleased
**Destination:** `inventory-service.items.reservation-released`

**Action:** Log event only (no customer notification)

**Reason:** Customer already received OrderCancelled notification.

---

## Spring Cloud Stream Configuration

### application.yml
```yaml
spring:
  cloud:
    function:
      definition: orderEventsConsumer;inventoryEventsConsumer
    stream:
      bindings:
        # Order Service Events
        orderEventsConsumer-in-0:
          destination: order-service.orders.created,order-service.orders.confirmed,order-service.orders.cancelled
          group: notification-service-group
          content-type: application/json
          consumer:
            max-attempts: 3
            back-off-initial-interval: 5000
            back-off-multiplier: 3.0
        # Inventory Service Events
        inventoryEventsConsumer-in-1:
          destination: inventory-service.items.reserved,inventory-service.items.reservation-failed,inventory-service.items.reservation-released
          group: notification-service-group
          content-type: application/json
          consumer:
            max-attempts: 3
            back-off-initial-interval: 5000
            back-off-multiplier: 3.0
      kafka:
        binder:
          brokers: localhost:29023
        bindings:
          orderEventsConsumer-in-0:
            consumer:
              enable-dlq: true
              dlq-name: notification-service.dlq
          inventoryEventsConsumer-in-1:
            consumer:
              enable-dlq: true
              dlq-name: notification-service.dlq
```

---

## Consumer Implementation

### Event Consumers
```java
@Configuration
public class NotificationEventConsumers {
    
    private final NotificationService notificationService;
    
    @Bean
    public Consumer<Message<BaseEvent>> orderEventsConsumer() {
        return message -> {
            BaseEvent event = message.getPayload();
            
            switch (event.getEventType()) {
                case "OrderCreated":
                    notificationService.handleOrderCreated((OrderCreatedEvent) event);
                    break;
                case "OrderConfirmed":
                    notificationService.handleOrderConfirmed((OrderConfirmedEvent) event);
                    break;
                case "OrderCancelled":
                    notificationService.handleOrderCancelled((OrderCancelledEvent) event);
                    break;
                default:
                    log.warn("Unknown event type: {}", event.getEventType());
            }
        };
    }
    
    @Bean
    public Consumer<Message<BaseEvent>> inventoryEventsConsumer() {
        return message -> {
            BaseEvent event = message.getPayload();
            
            switch (event.getEventType()) {
                case "ItemsReserved":
                    notificationService.handleItemsReserved((ItemsReservedEvent) event);
                    break;
                case "ReservationFailed":
                    notificationService.handleReservationFailed((ReservationFailedEvent) event);
                    break;
                case "ReservationReleased":
                    notificationService.handleReservationReleased((ReservationReleasedEvent) event);
                    break;
                default:
                    log.warn("Unknown event type: {}", event.getEventType());
            }
        };
    }
}
```

### Notification Service
```java
@Service
@Transactional
public class NotificationService {
    
    private final ProcessedEventRepository processedEventRepository;
    private final EmailService emailService;
    
    public void handleOrderCreated(OrderCreatedEvent event) {
        UUID eventId = event.getEventId();
        
        // Idempotency check
        if (processedEventRepository.existsByEventId(eventId)) {
            log.info("Duplicate event, skipping: eventId={}", eventId);
            return;
        }
        
        // Send email
        emailService.sendOrderConfirmationEmail(
            event.getPayload().getCustomerId(),
            event.getPayload().getOrderId(),
            event.getPayload().getTotalAmount()
        );
        
        // Mark as processed
        processedEventRepository.save(new ProcessedEvent(eventId, "OrderCreated", Instant.now()));
        
        log.info("Order created notification sent: orderId={}", event.getPayload().getOrderId());
    }
    
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        // Similar implementation
    }
    
    public void handleOrderCancelled(OrderCancelledEvent event) {
        // Similar implementation
    }
    
    public void handleItemsReserved(ItemsReservedEvent event) {
        // Optional: Send reservation success email
    }
    
    public void handleReservationFailed(ReservationFailedEvent event) {
        // Send stock unavailable email
    }
    
    public void handleReservationReleased(ReservationReleasedEvent event) {
        // Log only, no customer notification
        log.info("Reservation released: reservationId={}", event.getPayload().getReservationId());
        
        // Mark as processed to maintain idempotency
        processedEventRepository.save(new ProcessedEvent(
            event.getEventId(), 
            "ReservationReleased", 
            Instant.now()
        ));
    }
}
```

---

## Email Service Implementation

### Email Service Interface
```java
public interface EmailService {
    void sendOrderConfirmationEmail(UUID customerId, UUID orderId, String totalAmount);
    void sendOrderSuccessEmail(UUID customerId, UUID orderId);
    void sendOrderCancellationEmail(UUID customerId, UUID orderId, String reason);
    void sendStockUnavailableEmail(UUID customerId, UUID orderId, List<UnavailableItem> items);
}
```

### Mock Implementation (Development)
```java
@Service
@Profile("dev")
public class MockEmailService implements EmailService {
    
    @Override
    public void sendOrderConfirmationEmail(UUID customerId, UUID orderId, String totalAmount) {
        log.info("📧 [MOCK] Sending order confirmation email:");
        log.info("   To: customer-{}@example.com", customerId);
        log.info("   Order ID: {}", orderId);
        log.info("   Total: ${}", totalAmount);
    }
    
    // Other methods...
}
```

### Real Implementation (Production)
```java
@Service
@Profile("prod")
public class SmtpEmailService implements EmailService {
    
    private final JavaMailSender mailSender;
    
    @Override
    public void sendOrderConfirmationEmail(UUID customerId, UUID orderId, String totalAmount) {
        // Real SMTP email sending
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(getCustomerEmail(customerId));
        message.setSubject("Order Confirmation - " + orderId);
        message.setText(buildOrderConfirmationBody(orderId, totalAmount));
        
        mailSender.send(message);
        log.info("Email sent to customer: {}", customerId);
    }
}
```

---

## Idempotency

### Why Important for Notifications?
- Prevents duplicate emails to customers
- Same email received multiple times = bad user experience
- Email provider may flag as spam

### Implementation
```java
// Check before sending
if (processedEventRepository.existsByEventId(eventId)) {
    log.info("Email already sent for eventId={}, skipping", eventId);
    return;
}

// Send email
emailService.send(...);

// Mark as processed (in same transaction)
processedEventRepository.save(new ProcessedEvent(eventId, eventType, Instant.now()));
```

---

## Error Handling

### Retry Strategy
- Max 3 attempts
- Delays: 5s, 15s, 45s
- After 3 failures → DLQ

### Common Failures
- **SMTP connection failure:** Retry
- **Invalid email address:** DLQ (no retry)
- **Email provider rate limit:** Retry with backoff
- **Database transaction failure:** Retry

### Dead Letter Queue
Failed notifications go to `notification-service.dlq`

**Manual Review Process:**
1. Check DLQ topic
2. Identify failure reason
3. Fix issue (e.g., invalid email, service down)
4. Replay message manually or trigger resend

---

## Notification Channels (Future)

### Current
- ✅ Email

### Future Enhancements
- 📱 SMS notifications
- 🔔 Push notifications (mobile app)
- 💬 In-app notifications
- 📧 WhatsApp/Telegram messages

**Implementation Approach:**
- Same event consumers
- Multiple notification channels based on customer preferences
- Channel selection logic in NotificationService

---

## Monitoring

### Metrics to Track
- Number of notifications sent (by type)
- Notification delivery success rate
- Email bounce rate
- Average processing time
- DLQ message count

### Logging
```java
log.info("Notification sent: eventId={}, type={}, customerId={}, channel=EMAIL", 
    eventId, eventType, customerId);

log.warn("Notification failed: eventId={}, type={}, customerId={}, reason={}", 
    eventId, eventType, customerId, reason);
```

---

## Testing

### Integration Test
```java
@SpringBootTest
@EmbeddedKafka
class NotificationServiceIntegrationTest {
    
    @Autowired
    private InputDestination input;
    
    @MockBean
    private EmailService emailService;
    
    @Test
    void shouldSendEmailWhenOrderCreated() {
        // Given: OrderCreated event
        OrderCreatedEvent event = createOrderCreatedEvent();
        
        // When: Event received
        input.send(MessageBuilder.withPayload(event).build(), 
                   "order-service.orders.created");
        
        // Then: Email sent
        await().atMost(5, SECONDS).untilAsserted(() -> {
            verify(emailService, times(1)).sendOrderConfirmationEmail(
                any(UUID.class), any(UUID.class), anyString());
        });
    }
    
    @Test
    void shouldNotSendDuplicateEmail() {
        // Given: Same event sent twice
        OrderCreatedEvent event = createOrderCreatedEvent();
        
        // When: Send event twice
        input.send(MessageBuilder.withPayload(event).build(), 
                   "order-service.orders.created");
        input.send(MessageBuilder.withPayload(event).build(), 
                   "order-service.orders.created");
        
        // Then: Email sent only once (idempotency)
        await().atMost(5, SECONDS).untilAsserted(() -> {
            verify(emailService, times(1)).sendOrderConfirmationEmail(
                any(UUID.class), any(UUID.class), anyString());
        });
    }
}
```

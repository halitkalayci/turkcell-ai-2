# Idempotency Pattern

## Why Idempotency?

Message brokers typically provide "at-least-once" delivery guarantee:
- Messages may be delivered multiple times
- Network failures can cause retries
- Consumer crashes may lead to reprocessing
- Duplicate events WILL occur in production

**Solution:** Make all consumers idempotent - processing the same message multiple times produces the same result as processing it once.

## Implementation Strategy

### Database Table: processed_events

Each service maintains a table to track processed events:

```sql
CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_processed_events_type ON processed_events(event_type);
CREATE INDEX idx_processed_events_processed_at ON processed_events(processed_at);
```

### Processing Flow

```
1. Receive event from message broker
2. Extract eventId from event envelope
3. Check if eventId exists in processed_events table
4. IF exists:
     - Log "Duplicate event detected"
     - Skip processing
     - Acknowledge message
   ELSE:
     - Process business logic
     - Insert eventId into processed_events
     - Commit transaction (business logic + processed_events insert)
     - Acknowledge message
```

## Java Implementation

### Entity
```java
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {
    
    @Id
    private UUID eventId;
    
    @Column(nullable = false)
    private String eventType;
    
    @Column(nullable = false)
    private Instant processedAt;
    
    // Constructors, getters, setters
}
```

### Repository
```java
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
    
    boolean existsByEventId(UUID eventId);
}
```

### Consumer with Idempotency
```java
@Service
@Transactional
public class OrderEventHandler {
    
    private final ProcessedEventRepository processedEventRepository;
    private final OrderService orderService;
    
    public void handleOrderCreated(OrderCreatedEvent event) {
        UUID eventId = event.getEventId();
        
        // Check if already processed
        if (processedEventRepository.existsByEventId(eventId)) {
            log.info("Duplicate event detected, skipping: eventId={}", eventId);
            return;
        }
        
        // Process business logic
        orderService.reserveItemsForOrder(event.getPayload());
        
        // Mark as processed
        ProcessedEvent processedEvent = new ProcessedEvent(
            eventId,
            event.getEventType(),
            Instant.now()
        );
        processedEventRepository.save(processedEvent);
        
        log.info("Event processed successfully: eventId={}", eventId);
    }
}
```

### Spring Cloud Stream Consumer
```java
@Configuration
public class EventConsumers {
    
    private final OrderEventHandler orderEventHandler;
    
    @Bean
    public Consumer<Message<OrderCreatedEvent>> orderCreatedConsumer() {
        return message -> {
            OrderCreatedEvent event = message.getPayload();
            orderEventHandler.handleOrderCreated(event);
        };
    }
}
```

## Transaction Boundaries

**CRITICAL:** Idempotency check and business logic MUST be in the same transaction.

### ✅ Correct (Same Transaction)
```java
@Transactional
public void handleEvent(Event event) {
    if (processedEventRepository.existsByEventId(event.getEventId())) {
        return; // Skip
    }
    
    // Business logic
    businessService.doSomething(event);
    
    // Mark as processed
    processedEventRepository.save(new ProcessedEvent(...));
    
    // Both operations commit together
}
```

### ❌ Incorrect (Separate Transactions)
```java
// DON'T DO THIS
public void handleEvent(Event event) {
    if (isAlreadyProcessed(event.getEventId())) { // Transaction 1
        return;
    }
    
    processBusinessLogic(event); // Transaction 2
    markAsProcessed(event.getEventId()); // Transaction 3
    
    // Race condition possible!
}
```

**Problem:** Between checking and marking, another instance might process the same event.

## Idempotency Key Selection

### Option 1: Use Event ID (Recommended)
```java
UUID eventId = event.getEventId();
```
- ✅ Unique per event
- ✅ Provided by producer
- ✅ Guarantees no duplicate processing

### Option 2: Use Aggregate ID (Alternative)
```java
UUID orderId = event.getAggregateId();
```
- ⚠️ Only if business logic is naturally idempotent
- ⚠️ May miss updates to same aggregate
- ❌ Not recommended for general use

**Best Practice:** Always use `eventId` as the idempotency key.

## Edge Cases

### Case 1: Consumer Crash After Processing, Before Acknowledging
```
1. Consumer receives event
2. Business logic executes successfully
3. EventId saved to processed_events
4. ✅ Transaction commits
5. ❌ Consumer crashes before acknowledging message
6. Message broker redelivers event
7. Consumer checks processed_events → Already exists
8. Skip processing (idempotent)
9. Acknowledge message
```
**Result:** No duplicate processing ✅

### Case 2: Database Commit Failure
```
1. Consumer receives event
2. Business logic executes
3. ❌ Database transaction fails
4. No commit happens
5. Exception thrown
6. Spring Cloud Stream retries (3 times)
7. Eventually goes to DLQ if all retries fail
```
**Result:** No partial processing ✅

### Case 3: Duplicate Events from Producer
```
1. Producer publishes event (eventId: 123)
2. Network issue, producer retries
3. Producer publishes same event again (eventId: 123)
4. Consumer 1 processes eventId 123 → Success
5. Consumer 2 receives eventId 123 → Checks → Already processed → Skip
```
**Result:** Processed exactly once ✅

## Performance Considerations

### Index Strategy
```sql
-- Primary key lookup is fast
CREATE INDEX idx_processed_events_type ON processed_events(event_type);

-- For queries by time range
CREATE INDEX idx_processed_events_processed_at ON processed_events(processed_at);
```

### Cleanup Strategy (Optional)
If processed_events table grows too large:

```java
@Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
public void cleanupOldProcessedEvents() {
    Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
    processedEventRepository.deleteByProcessedAtBefore(cutoff);
    log.info("Cleaned up processed events older than 30 days");
}
```

**Note:** Only cleanup if table size becomes an issue. Keeping history is useful for auditing.

## Testing Idempotency

### Unit Test
```java
@Test
void shouldSkipDuplicateEvent() {
    // Given: Event already processed
    UUID eventId = UUID.randomUUID();
    ProcessedEvent processed = new ProcessedEvent(eventId, "OrderCreated", Instant.now());
    processedEventRepository.save(processed);
    
    OrderCreatedEvent event = new OrderCreatedEvent(...);
    event.setEventId(eventId);
    
    // When: Process duplicate event
    orderEventHandler.handleOrderCreated(event);
    
    // Then: Business logic NOT called
    verify(orderService, never()).reserveItemsForOrder(any());
}

@Test
void shouldProcessNewEvent() {
    // Given: New event
    OrderCreatedEvent event = new OrderCreatedEvent(...);
    UUID eventId = event.getEventId();
    
    // When: Process event
    orderEventHandler.handleOrderCreated(event);
    
    // Then: Business logic called AND event marked as processed
    verify(orderService, times(1)).reserveItemsForOrder(any());
    assertTrue(processedEventRepository.existsByEventId(eventId));
}
```

### Integration Test
```java
@SpringBootTest
@EmbeddedKafka
class IdempotencyIntegrationTest {
    
    @Test
    void shouldHandleDuplicateMessagesIdempotently() {
        // Given: Same event sent twice
        OrderCreatedEvent event = createOrderCreatedEvent();
        
        // When: Send same event multiple times
        sendEvent(event);
        sendEvent(event); // Duplicate
        sendEvent(event); // Duplicate
        
        // Then: Processed only once
        await().atMost(5, SECONDS).until(() -> 
            orderRepository.count() == 1
        );
    }
}
```

## Monitoring

### Metrics to Track
- Number of duplicate events detected (per event type)
- Processing time impact of idempotency check
- Size of processed_events table

### Logging
```java
if (processedEventRepository.existsByEventId(eventId)) {
    log.warn("Duplicate event detected: eventId={}, eventType={}, correlationId={}", 
        eventId, event.getEventType(), event.getCorrelationId());
    return;
}

log.info("Processing event: eventId={}, eventType={}, aggregateId={}, correlationId={}", 
    eventId, event.getEventType(), event.getAggregateId(), event.getCorrelationId());
```

## Summary

✅ **DO:**
- Use `eventId` as idempotency key
- Check + process + mark in same transaction
- Index the processed_events table
- Log duplicate detections
- Test idempotency scenarios

❌ **DON'T:**
- Skip idempotency checks
- Split check and mark into separate transactions
- Use business IDs as idempotency keys (unless naturally idempotent)
- Acknowledge message before committing transaction

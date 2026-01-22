# Messaging Configuration

## Spring Cloud Stream Overview
This project uses Spring Cloud Stream to abstract message broker implementation.
Services can switch between Kafka, RabbitMQ, or other brokers via configuration only.

## Current Configuration

### Message Broker
- **Type:** Apache Kafka
- **Bootstrap Servers:** `localhost:29092`
- **Environment:** Development (single-node)
- **Serialization:** JSON

### Binder
- **Active Binder:** Kafka
- **Alternative Binders:** RabbitMQ, AWS Kinesis, Azure Event Hubs, etc.

## Destination Naming Convention

### Regular Destinations
Pattern: `{service-name}.{domain}.{event-type}`

Examples:
- `order-service.orders.created`
- `order-service.orders.confirmed`
- `order-service.orders.cancelled`
- `inventory-service.items.reserved`
- `inventory-service.items.reservation-failed`
- `inventory-service.items.reservation-released`

### Dead Letter Queue Destinations
Pattern: `{service-name}.dlq`

Examples:
- `order-service.dlq`
- `inventory-service.dlq`
- `notification-service.dlq`

## Destination Configuration

| Destination | Partitions | Retention | Producer | Consumers |
|-------------|-----------|-----------|----------|-----------|
| order-service.orders.created | 3 | 7 days | Order Service | Inventory, Notification |
| order-service.orders.confirmed | 3 | 7 days | Order Service | Notification |
| order-service.orders.cancelled | 3 | 7 days | Order Service | Inventory, Notification |
| inventory-service.items.reserved | 3 | 7 days | Inventory Service | Order, Notification |
| inventory-service.items.reservation-failed | 3 | 7 days | Inventory Service | Order, Notification |
| inventory-service.items.reservation-released | 3 | 7 days | Inventory Service | Notification |

## Consumer Groups

| Consumer Group | Service | Subscribed Destinations |
|----------------|---------|------------------------|
| order-service-group | Order Service | inventory-service.items.* |
| inventory-service-group | Inventory Service | order-service.orders.* |
| notification-service-group | Notification Service | *.*.* (all events) |

## Spring Cloud Stream Configuration

### Order Service (application.yml)
```yaml
spring:
  cloud:
    function:
      definition: orderCreatedSupplier;orderConfirmedSupplier;orderCancelledSupplier;itemsReservedConsumer;reservationFailedConsumer
    stream:
      bindings:
        # Producers
        orderCreatedSupplier-out-0:
          destination: order-service.orders.created
          content-type: application/json
        orderConfirmedSupplier-out-0:
          destination: order-service.orders.confirmed
          content-type: application/json
        orderCancelledSupplier-out-0:
          destination: order-service.orders.cancelled
          content-type: application/json
        # Consumers
        itemsReservedConsumer-in-0:
          destination: inventory-service.items.reserved
          group: order-service-group
          content-type: application/json
          consumer:
            max-attempts: 3
            back-off-initial-interval: 5000
            back-off-multiplier: 3.0
        reservationFailedConsumer-in-0:
          destination: inventory-service.items.reservation-failed
          group: order-service-group
          content-type: application/json
          consumer:
            max-attempts: 3
            back-off-initial-interval: 5000
            back-off-multiplier: 3.0
      kafka:
        binder:
          brokers: localhost:29092
        bindings:
          itemsReservedConsumer-in-0:
            consumer:
              enable-dlq: true
              dlq-name: order-service.dlq
          reservationFailedConsumer-in-0:
            consumer:
              enable-dlq: true
              dlq-name: order-service.dlq
```

### Inventory Service (application.yml)
```yaml
spring:
  cloud:
    function:
      definition: itemsReservedSupplier;reservationFailedSupplier;reservationReleasedSupplier;orderCreatedConsumer;orderCancelledConsumer
    stream:
      bindings:
        # Producers
        itemsReservedSupplier-out-0:
          destination: inventory-service.items.reserved
          content-type: application/json
        reservationFailedSupplier-out-0:
          destination: inventory-service.items.reservation-failed
          content-type: application/json
        reservationReleasedSupplier-out-0:
          destination: inventory-service.items.reservation-released
          content-type: application/json
        # Consumers
        orderCreatedConsumer-in-0:
          destination: order-service.orders.created
          group: inventory-service-group
          content-type: application/json
          consumer:
            max-attempts: 3
            back-off-initial-interval: 5000
            back-off-multiplier: 3.0
        orderCancelledConsumer-in-0:
          destination: order-service.orders.cancelled
          group: inventory-service-group
          content-type: application/json
          consumer:
            max-attempts: 3
            back-off-initial-interval: 5000
            back-off-multiplier: 3.0
      kafka:
        binder:
          brokers: localhost:29092
        bindings:
          orderCreatedConsumer-in-0:
            consumer:
              enable-dlq: true
              dlq-name: inventory-service.dlq
          orderCancelledConsumer-in-0:
            consumer:
              enable-dlq: true
              dlq-name: inventory-service.dlq
```

### Notification Service (application.yml)
```yaml
spring:
  cloud:
    function:
      definition: allEventsConsumer
    stream:
      bindings:
        allEventsConsumer-in-0:
          destination: order-service.orders.*,inventory-service.items.*
          group: notification-service-group
          content-type: application/json
          consumer:
            max-attempts: 3
            back-off-initial-interval: 5000
            back-off-multiplier: 3.0
      kafka:
        binder:
          brokers: localhost:29092
        bindings:
          allEventsConsumer-in-0:
            consumer:
              enable-dlq: true
              dlq-name: notification-service.dlq
```

## Switching to RabbitMQ

To switch from Kafka to RabbitMQ:

### 1. Update Dependencies (pom.xml)
Remove:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-stream-binder-kafka</artifactId>
</dependency>
```

Add:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-stream-binder-rabbit</artifactId>
</dependency>
```

### 2. Update Configuration (application.yml)
Replace:
```yaml
spring:
  cloud:
    stream:
      kafka:
        binder:
          brokers: localhost:29092
```

With:
```yaml
spring:
  cloud:
    stream:
      rabbit:
        binder:
          host: localhost
          port: 5672
          username: guest
          password: guest
```

### 3. No Code Changes Required!
All application code remains the same due to Spring Cloud Stream abstraction.

## Functional Programming Model

Spring Cloud Stream uses functional interfaces:
- **Supplier<T>:** Producer (emits messages)
- **Function<T,R>:** Processor (transforms messages)
- **Consumer<T>:** Consumer (receives messages)

### Example: Producer
```java
@Configuration
public class EventProducers {
    
    @Bean
    public Supplier<Flux<Message<OrderCreatedEvent>>> orderCreatedSupplier(OutboxEventPublisher publisher) {
        return () -> publisher.pollAndPublish("OrderCreated");
    }
}
```

### Example: Consumer
```java
@Configuration
public class EventConsumers {
    
    @Bean
    public Consumer<Message<OrderCreatedEvent>> orderCreatedConsumer(OrderEventHandler handler) {
        return message -> {
            handler.handleOrderCreated(message.getPayload());
            // Manual acknowledgment if needed
        };
    }
}
```

## Message Headers

Spring Cloud Stream automatically adds headers:
- `id` - Message ID
- `timestamp` - Message timestamp
- `contentType` - Message content type

Custom headers can be added:
```java
Message<OrderCreatedEvent> message = MessageBuilder
    .withPayload(event)
    .setHeader("correlationId", correlationId)
    .build();
```

## Error Handling

### Retry Configuration
```yaml
consumer:
  max-attempts: 3
  back-off-initial-interval: 5000
  back-off-multiplier: 3.0
```

- Attempt 1: Immediate
- Attempt 2: After 5 seconds
- Attempt 3: After 15 seconds (5s * 3.0)
- After 3 attempts: Send to DLQ

### Dead Letter Queue
Failed messages are sent to DLQ with error information:
- Original message payload
- Exception stack trace
- Retry count
- Original destination

## Testing

### With Embedded Broker (Kafka)
```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka
class EventIntegrationTest {
    // Test implementation
}
```

### With Test Binder
```java
@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
class EventTest {
    @Autowired
    private InputDestination input;
    
    @Autowired
    private OutputDestination output;
    
    // Test implementation
}
```

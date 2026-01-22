package com.ecommerce.inventoryservice.infrastructure.messaging.publisher;

import com.ecommerce.inventoryservice.infrastructure.messaging.outbox.OutboxEvent;
import com.ecommerce.inventoryservice.infrastructure.messaging.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduled publisher that polls the outbox table and publishes events to Kafka.
 * Part of Transactional Outbox Pattern.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {
    
    private final OutboxEventRepository outboxEventRepository;
    private final StreamBridge streamBridge;
    
    /**
     * Poll outbox table every 10 seconds and publish pending events.
     */
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
            OutboxEvent.OutboxEventStatus.PENDING
        );
        
        if (pendingEvents.isEmpty()) {
            return;
        }
        
        log.info("Found {} pending events to publish", pendingEvents.size());
        
        for (OutboxEvent event : pendingEvents) {
            try {
                publishEvent(event);
            } catch (Exception e) {
                log.error("Failed to publish event: eventId={}, eventType={}", 
                    event.getId(), event.getEventType(), e);
                event.markAsFailed();
                outboxEventRepository.save(event);
            }
        }
    }
    
    private void publishEvent(OutboxEvent event) {
        String destination = buildDestination(event.getEventType());
        
        boolean sent = streamBridge.send(destination, event.getPayload());
        
        if (sent) {
            event.markAsPublished();
            outboxEventRepository.save(event);
            
            log.info("Event published successfully: eventId={}, eventType={}, destination={}",
                event.getId(), event.getEventType(), destination);
        } else {
            throw new RuntimeException("Failed to send message to destination: " + destination);
        }
    }
    
    private String buildDestination(String eventType) {
        // Convert eventType to destination name
        return switch (eventType) {
            case "ItemsReserved" -> "inventory-service.items.reserved";
            case "ReservationFailed" -> "inventory-service.items.reservation-failed";
            case "ReservationReleased" -> "inventory-service.items.reservation-released";
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
}

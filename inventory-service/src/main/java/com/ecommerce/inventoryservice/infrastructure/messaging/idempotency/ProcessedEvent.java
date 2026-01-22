package com.ecommerce.inventoryservice.infrastructure.messaging.idempotency;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity to track processed events for idempotency.
 */
@Entity
@Table(name = "processed_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {
    
    @Id
    private UUID eventId;
    
    @Column(nullable = false)
    private String eventType;
    
    @Column(nullable = false)
    private Instant processedAt;
    
    public ProcessedEvent(UUID eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = Instant.now();
    }
}

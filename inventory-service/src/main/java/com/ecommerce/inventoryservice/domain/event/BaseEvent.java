package com.ecommerce.inventoryservice.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events.
 * Provides standard event envelope structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {
    
    private UUID eventId;
    private String eventType;
    private Instant eventTimestamp;
    private UUID correlationId;
    private UUID aggregateId;
    
    protected BaseEvent(String eventType, UUID aggregateId, UUID correlationId) {
        this.eventId = UUID.randomUUID();
        this.eventType = eventType;
        this.eventTimestamp = Instant.now();
        this.aggregateId = aggregateId;
        this.correlationId = correlationId;
    }
}

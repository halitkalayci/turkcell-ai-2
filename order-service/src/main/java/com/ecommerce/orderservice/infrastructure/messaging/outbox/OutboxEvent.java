package com.ecommerce.orderservice.infrastructure.messaging.outbox;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox event entity for Transactional Outbox Pattern.
 * Stores events to be published to message broker.
 */
@Entity
@Table(name = "outbox_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    
    @Id
    private UUID id;
    
    @Column(nullable = false)
    private UUID aggregateId;
    
    @Column(nullable = false)
    private String eventType;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column
    private Instant publishedAt;
    
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OutboxEventStatus status;
    
    public enum OutboxEventStatus {
        PENDING,
        PUBLISHED,
        FAILED
    }
    
    public static OutboxEvent create(UUID aggregateId, String eventType, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setCreatedAt(Instant.now());
        event.setStatus(OutboxEventStatus.PENDING);
        return event;
    }
    
    public void markAsPublished() {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }
    
    public void markAsFailed() {
        this.status = OutboxEventStatus.FAILED;
    }
}

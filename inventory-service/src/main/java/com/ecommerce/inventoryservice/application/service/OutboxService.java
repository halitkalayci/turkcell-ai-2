package com.ecommerce.inventoryservice.application.service;

import com.ecommerce.inventoryservice.domain.event.BaseEvent;
import com.ecommerce.inventoryservice.infrastructure.messaging.outbox.OutboxEvent;
import com.ecommerce.inventoryservice.infrastructure.messaging.outbox.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for saving events to the outbox table.
 * Part of Transactional Outbox Pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {
    
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Save an event to the outbox table within the same transaction as business logic.
     * Event will be published later by the OutboxEventPublisher.
     */
    @Transactional
    public void saveToOutbox(BaseEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            
            OutboxEvent outboxEvent = OutboxEvent.create(
                event.getAggregateId(),
                event.getEventType(),
                payload
            );
            
            outboxEventRepository.save(outboxEvent);
            
            log.info("Event saved to outbox: eventId={}, eventType={}, aggregateId={}",
                event.getEventId(), event.getEventType(), event.getAggregateId());
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event to JSON: eventType={}", event.getEventType(), e);
            throw new RuntimeException("Failed to save event to outbox", e);
        }
    }
}

package com.ecommerce.inventoryservice.infrastructure.messaging.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for managing processed events.
 */
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
    
    /**
     * Check if an event has already been processed.
     */
    boolean existsByEventId(UUID eventId);
}

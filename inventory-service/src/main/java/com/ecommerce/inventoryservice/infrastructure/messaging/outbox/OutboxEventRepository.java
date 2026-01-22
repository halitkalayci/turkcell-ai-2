package com.ecommerce.inventoryservice.infrastructure.messaging.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing outbox events.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    
    /**
     * Find all pending events to be published.
     */
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxEventStatus status);
}

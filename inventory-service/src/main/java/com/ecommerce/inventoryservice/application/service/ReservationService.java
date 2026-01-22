package com.ecommerce.inventoryservice.application.service;

import com.ecommerce.inventoryservice.domain.port.InventoryRepository;
import com.ecommerce.inventoryservice.domain.port.ReservationRepository;
import com.ecommerce.inventoryservice.domain.event.ItemsReservedEvent;
import com.ecommerce.inventoryservice.domain.event.OrderCreatedEvent;
import com.ecommerce.inventoryservice.domain.event.ReservationFailedEvent;
import com.ecommerce.inventoryservice.domain.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.domain.model.InventoryItem;
import com.ecommerce.inventoryservice.domain.model.Reservation;
import com.ecommerce.inventoryservice.domain.model.ReservationItem;
import com.ecommerce.inventoryservice.infrastructure.messaging.idempotency.ProcessedEvent;
import com.ecommerce.inventoryservice.infrastructure.messaging.idempotency.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling reservation operations.
 * Orchestrates domain logic and infrastructure.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {
    
    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxService outboxService;
    
    /**
     * Handle OrderCreated event and reserve items.
     * Idempotent - checks if event already processed.
     */
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        UUID eventId = event.getEventId();
        UUID orderId = event.getPayload().getOrderId();
        UUID correlationId = event.getCorrelationId();
        
        log.info("Handling OrderCreated event: eventId={}, orderId={}, correlationId={}", 
            eventId, orderId, correlationId);
        
        // Idempotency check
        if (processedEventRepository.existsByEventId(eventId)) {
            log.info("Event already processed, skipping: eventId={}", eventId);
            return;
        }
        
        try {
            // Reserve items
            Reservation reservation = reserveItemsForOrder(event.getPayload(), correlationId);
            
            // Publish ItemsReserved event
            publishItemsReservedEvent(reservation, correlationId);
            
            // Mark event as processed
            processedEventRepository.save(new ProcessedEvent(eventId, event.getEventType()));
            
            log.info("Order reservation completed successfully: orderId={}, reservationId={}", 
                orderId, reservation.getId());
            
        } catch (InsufficientStockException e) {
            log.warn("Insufficient stock for order: orderId={}, reason={}", orderId, e.getMessage());
            
            // Mark as processed to avoid retry (business failure, not technical)
            processedEventRepository.save(new ProcessedEvent(eventId, event.getEventType()));
            
            // Publish ReservationFailed event
            publishReservationFailedEvent(event.getPayload(), "INSUFFICIENT_STOCK", e.getMessage(), correlationId);
            
        } catch (IllegalArgumentException e) {
            log.warn("Product not found for order: orderId={}, reason={}", orderId, e.getMessage());
            
            // Mark as processed to avoid retry (business failure, not technical)
            processedEventRepository.save(new ProcessedEvent(eventId, event.getEventType()));
            
            // Publish ReservationFailed event
            publishReservationFailedEvent(event.getPayload(), "PRODUCT_NOT_FOUND", e.getMessage(), correlationId);
            
        } catch (Exception e) {
            log.error("Failed to process OrderCreated event: eventId={}, orderId={}", 
                eventId, orderId, e);
            throw e; // Will trigger retry via Spring Cloud Stream
        }
    }
    
    private Reservation reserveItemsForOrder(OrderCreatedEvent.OrderCreatedPayload payload, UUID correlationId) {
        UUID orderId = payload.getOrderId();
        List<OrderCreatedEvent.OrderItem> orderItems = payload.getItems();
        
        // Check if reservation already exists for this order
        Optional<Reservation> existingReservation = reservationRepository.findByOrderId(orderId);
        if (existingReservation.isPresent()) {
            log.warn("Reservation already exists for order: orderId={}", orderId);
            return existingReservation.get();
        }
        
        // Reserve each item
        List<ReservationItem> reservationItems = new ArrayList<>();
        
        for (OrderCreatedEvent.OrderItem orderItem : orderItems) {
            UUID productId = orderItem.getProductId();
            Integer quantity = orderItem.getQuantity();
            
            // Get inventory item
            InventoryItem inventoryItem = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
            
            // Reserve stock (domain logic)
            inventoryItem.reserve(quantity);
            
            // Save updated inventory
            inventoryRepository.save(inventoryItem);
            
            // Create reservation item
            reservationItems.add(new ReservationItem(
                productId,
                quantity
            ));
            
            log.info("Reserved {} units of product {}", quantity, productId);
        }
        
        // Create reservation
        UUID reservationId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(30, ChronoUnit.MINUTES); // 30 min TTL
        
        Reservation reservation = new Reservation(
            reservationId,
            orderId,
            reservationItems,
            now,
            expiresAt
        );
        
        // Persist reservation
        Reservation savedReservation = reservationRepository.save(reservation);
        
        log.info("Reservation created: reservationId={}, orderId={}, itemCount={}", 
            reservationId, orderId, reservationItems.size());
        
        return savedReservation;
    }
    
    private void publishItemsReservedEvent(Reservation reservation, UUID correlationId) {
        List<ItemsReservedEvent.ReservedItem> eventItems = reservation.getItems().stream()
            .map(item -> new ItemsReservedEvent.ReservedItem(
                item.getProductId(),
                item.getQuantity(),
                item.getQuantity() // reservedQuantity = quantity in this case
            ))
            .toList();
        
        ItemsReservedEvent event = new ItemsReservedEvent(
            reservation.getId(),
            reservation.getOrderId(),
            eventItems,
            correlationId
        );
        
        outboxService.saveToOutbox(event);
        
        log.info("ItemsReserved event saved to outbox: reservationId={}, orderId={}, correlationId={}", 
            reservation.getId(), reservation.getOrderId(), correlationId);
    }
    
    private void publishReservationFailedEvent(OrderCreatedEvent.OrderCreatedPayload payload, 
                                               String reason, String errorMessage, UUID correlationId) {
        UUID orderId = payload.getOrderId();
        List<OrderCreatedEvent.OrderItem> orderItems = payload.getItems();
        
        List<ReservationFailedEvent.UnavailableItem> unavailableItems = new ArrayList<>();
        
        for (OrderCreatedEvent.OrderItem orderItem : orderItems) {
            UUID productId = orderItem.getProductId();
            Integer requestedQuantity = orderItem.getQuantity();
            
            // Try to get available quantity
            Integer availableQuantity = 0;
            Optional<InventoryItem> inventoryItem = inventoryRepository.findByProductId(productId);
            if (inventoryItem.isPresent()) {
                availableQuantity = inventoryItem.get().getAvailableQuantity();
            }
            
            unavailableItems.add(new ReservationFailedEvent.UnavailableItem(
                productId,
                requestedQuantity,
                availableQuantity
            ));
        }
        
        ReservationFailedEvent event = new ReservationFailedEvent(
            orderId,
            reason,
            unavailableItems,
            correlationId
        );
        
        outboxService.saveToOutbox(event);
        
        log.info("ReservationFailed event saved to outbox: orderId={}, reason={}, correlationId={}", 
            orderId, reason, correlationId);
    }
}

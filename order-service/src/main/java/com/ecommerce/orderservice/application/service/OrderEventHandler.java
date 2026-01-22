package com.ecommerce.orderservice.application.service;

import com.ecommerce.orderservice.domain.event.ItemsReservedEvent;
import com.ecommerce.orderservice.domain.event.ReservationFailedEvent;
import com.ecommerce.orderservice.domain.model.Order;
import com.ecommerce.orderservice.domain.model.OrderStatus;
import com.ecommerce.orderservice.domain.port.OrderRepository;
import com.ecommerce.orderservice.infrastructure.messaging.idempotency.ProcessedEvent;
import com.ecommerce.orderservice.infrastructure.messaging.idempotency.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service for handling inventory-related events.
 * Updates order status based on reservation results.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventHandler {
    
    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxService outboxService;
    
    /**
     * Handle ItemsReserved event from Inventory Service.
     * Updates order status to CONFIRMED.
     * Idempotent - checks if event already processed.
     */
    @Transactional
    public void handleItemsReserved(ItemsReservedEvent event) {
        UUID eventId = event.getEventId();
        UUID orderId = event.getPayload().getOrderId();
        UUID correlationId = event.getCorrelationId();
        
        log.info("Handling ItemsReserved event: eventId={}, orderId={}, correlationId={}", 
            eventId, orderId, correlationId);
        
        // Idempotency check
        if (processedEventRepository.existsByEventId(eventId)) {
            log.info("Event already processed, skipping: eventId={}", eventId);
            return;
        }
        
        try {
            // Find order
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
            
            // Update status to CONFIRMED
            order.updateStatus(OrderStatus.CONFIRMED);
            Order savedOrder = orderRepository.save(order);
            
            log.info("Order confirmed: orderId={}, status={}", orderId, savedOrder.getStatus());
            
            // Mark event as processed
            processedEventRepository.save(new ProcessedEvent(eventId, event.getEventType()));
            
            // TODO: Publish OrderConfirmed event (future enhancement)
            
        } catch (Exception e) {
            log.error("Failed to process ItemsReserved event: eventId={}, orderId={}", 
                eventId, orderId, e);
            throw e; // Will trigger retry via Spring Cloud Stream
        }
    }
    
    /**
     * Handle ReservationFailed event from Inventory Service.
     * Cancels the order.
     * Idempotent - checks if event already processed.
     */
    @Transactional
    public void handleReservationFailed(ReservationFailedEvent event) {
        UUID eventId = event.getEventId();
        UUID orderId = event.getPayload().getOrderId();
        String reason = event.getPayload().getReason();
        UUID correlationId = event.getCorrelationId();
        
        log.info("Handling ReservationFailed event: eventId={}, orderId={}, reason={}, correlationId={}", 
            eventId, orderId, reason, correlationId);
        
        // Idempotency check
        if (processedEventRepository.existsByEventId(eventId)) {
            log.info("Event already processed, skipping: eventId={}", eventId);
            return;
        }
        
        try {
            // Find order
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
            
            // Cancel order
            order.cancel();
            Order savedOrder = orderRepository.save(order);
            
            log.info("Order cancelled due to reservation failure: orderId={}, status={}, reason={}", 
                orderId, savedOrder.getStatus(), reason);
            
            // Mark event as processed
            processedEventRepository.save(new ProcessedEvent(eventId, event.getEventType()));
            
            // TODO: Publish OrderCancelled event (future enhancement)
            
        } catch (Exception e) {
            log.error("Failed to process ReservationFailed event: eventId={}, orderId={}", 
                eventId, orderId, e);
            throw e; // Will trigger retry via Spring Cloud Stream
        }
    }
}

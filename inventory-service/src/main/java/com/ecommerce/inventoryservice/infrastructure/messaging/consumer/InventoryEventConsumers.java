package com.ecommerce.inventoryservice.infrastructure.messaging.consumer;

import com.ecommerce.inventoryservice.application.service.ReservationService;
import com.ecommerce.inventoryservice.domain.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

/**
 * Spring Cloud Stream consumers for inventory service.
 * Uses functional programming model.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumers {
    
    private final ReservationService reservationService;
    
    /**
     * Consumer for OrderCreated events from Order Service.
     */
    @Bean
    public Consumer<Message<OrderCreatedEvent>> orderCreatedConsumer() {
        return message -> {
            OrderCreatedEvent event = message.getPayload();
            
            log.info("Received OrderCreated event: eventId={}, orderId={}", 
                event.getEventId(), event.getPayload().getOrderId());
            
            try {
                reservationService.handleOrderCreated(event);
            } catch (Exception e) {
                log.error("Error processing OrderCreated event: eventId={}", 
                    event.getEventId(), e);
                throw e; // Trigger retry
            }
        };
    }
}

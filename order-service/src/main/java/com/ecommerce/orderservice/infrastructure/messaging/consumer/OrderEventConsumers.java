package com.ecommerce.orderservice.infrastructure.messaging.consumer;

import com.ecommerce.orderservice.application.service.OrderEventHandler;
import com.ecommerce.orderservice.domain.event.ItemsReservedEvent;
import com.ecommerce.orderservice.domain.event.ReservationFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

/**
 * Spring Cloud Stream consumers for order service.
 * Uses functional programming model.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumers {
    
    private final OrderEventHandler orderEventHandler;
    
    /**
     * Consumer for ItemsReserved events from Inventory Service.
     */
    @Bean
    public Consumer<Message<ItemsReservedEvent>> itemsReservedConsumer() {
        return message -> {
            ItemsReservedEvent event = message.getPayload();
            
            log.info("Received ItemsReserved event: eventId={}, reservationId={}, orderId={}", 
                event.getEventId(), 
                event.getPayload().getReservationId(), 
                event.getPayload().getOrderId());
            
            try {
                orderEventHandler.handleItemsReserved(event);
            } catch (Exception e) {
                log.error("Error processing ItemsReserved event: eventId={}", 
                    event.getEventId(), e);
                throw e; // Trigger retry
            }
        };
    }
    
    /**
     * Consumer for ReservationFailed events from Inventory Service.
     */
    @Bean
    public Consumer<Message<ReservationFailedEvent>> reservationFailedConsumer() {
        return message -> {
            ReservationFailedEvent event = message.getPayload();
            
            log.info("Received ReservationFailed event: eventId={}, orderId={}, reason={}", 
                event.getEventId(), 
                event.getPayload().getOrderId(),
                event.getPayload().getReason());
            
            try {
                orderEventHandler.handleReservationFailed(event);
            } catch (Exception e) {
                log.error("Error processing ReservationFailed event: eventId={}", 
                    event.getEventId(), e);
                throw e; // Trigger retry
            }
        };
    }
}

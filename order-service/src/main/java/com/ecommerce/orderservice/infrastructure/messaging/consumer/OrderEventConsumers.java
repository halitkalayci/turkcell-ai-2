package com.ecommerce.orderservice.infrastructure.messaging.consumer;

import com.ecommerce.orderservice.domain.event.ItemsReservedEvent;
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
    
    /**
     * Consumer for ItemsReserved events from Inventory Service.
     * Will be implemented fully when order confirmation logic is added.
     */
    @Bean
    public Consumer<Message<ItemsReservedEvent>> itemsReservedConsumer() {
        return message -> {
            ItemsReservedEvent event = message.getPayload();
            
            log.info("Received ItemsReserved event: eventId={}, reservationId={}, orderId={}", 
                event.getEventId(), 
                event.getPayload().getReservationId(), 
                event.getPayload().getOrderId());
            
            // TODO: Update order status to CONFIRMED
            // Will be implemented in next iteration
            
            log.info("ItemsReserved event processed (placeholder): orderId={}", 
                event.getPayload().getOrderId());
        };
    }
}

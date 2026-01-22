package com.ecommerce.orderservice.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Event published when an order is created.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderCreatedEvent extends BaseEvent {
    
    private OrderCreatedPayload payload;
    
    public OrderCreatedEvent(UUID orderId, UUID customerId, List<OrderItem> items, BigDecimal totalAmount, UUID correlationId) {
        super("OrderCreated", orderId, correlationId);
        this.payload = new OrderCreatedPayload(orderId, customerId, items, totalAmount.toString(), "PENDING");
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCreatedPayload {
        private UUID orderId;
        private UUID customerId;
        private List<OrderItem> items;
        private String totalAmount; // BigDecimal as String
        private String status;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private UUID productId;
        private Integer quantity;
        private String price; // BigDecimal as String
    }
}

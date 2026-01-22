package com.ecommerce.orderservice.domain.exception;

import java.util.UUID;

/**
 * Exception thrown when a requested order cannot be found.
 * Results in HTTP 404 Not Found response.
 */
public class OrderNotFoundException extends RuntimeException {
    
    private final UUID orderId;
    
    public OrderNotFoundException(UUID orderId) {
        super(String.format("Order not found with ID: %s", orderId));
        this.orderId = orderId;
    }
    
    public OrderNotFoundException(UUID orderId, String message) {
        super(message);
        this.orderId = orderId;
    }
    
    public UUID getOrderId() {
        return orderId;
    }
}

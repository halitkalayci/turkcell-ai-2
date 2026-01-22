package com.ecommerce.orderservice.domain.exception;

import com.ecommerce.orderservice.domain.model.OrderStatus;

import java.util.UUID;

/**
 * Exception thrown when attempting to cancel an order that cannot be cancelled.
 * Business rule: Only PREPARING and PENDING orders can be cancelled.
 * Results in HTTP 422 Unprocessable Entity response.
 */
public class CannotCancelOrderException extends RuntimeException {
    
    private final UUID orderId;
    private final OrderStatus currentStatus;
    
    public CannotCancelOrderException(UUID orderId, OrderStatus currentStatus) {
        super(String.format(
            "Cannot cancel order %s with status %s. Only PREPARING and PENDING orders can be cancelled.",
            orderId, currentStatus
        ));
        this.orderId = orderId;
        this.currentStatus = currentStatus;
    }
    
    public CannotCancelOrderException(UUID orderId, OrderStatus currentStatus, String message) {
        super(message);
        this.orderId = orderId;
        this.currentStatus = currentStatus;
    }
    
    public UUID getOrderId() {
        return orderId;
    }
    
    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }
}

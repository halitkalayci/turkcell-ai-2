package com.ecommerce.orderservice.domain.exception;

import com.ecommerce.orderservice.domain.model.OrderStatus;

import java.util.UUID;

/**
 * Exception thrown when attempting an invalid order state transition.
 * Business rule: State transitions must follow valid lifecycle rules.
 * Results in HTTP 422 Unprocessable Entity response.
 */
public class InvalidOrderStateException extends RuntimeException {
    
    private final UUID orderId;
    private final OrderStatus currentStatus;
    private final OrderStatus attemptedStatus;
    
    public InvalidOrderStateException(UUID orderId, OrderStatus currentStatus, OrderStatus attemptedStatus) {
        super(String.format(
            "Invalid state transition for order %s: cannot change from %s to %s",
            orderId, currentStatus, attemptedStatus
        ));
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.attemptedStatus = attemptedStatus;
    }
    
    public InvalidOrderStateException(String message) {
        super(message);
        this.orderId = null;
        this.currentStatus = null;
        this.attemptedStatus = null;
    }
    
    public UUID getOrderId() {
        return orderId;
    }
    
    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }
    
    public OrderStatus getAttemptedStatus() {
        return attemptedStatus;
    }
}

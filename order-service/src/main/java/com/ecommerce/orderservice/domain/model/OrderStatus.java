package com.ecommerce.orderservice.domain.model;

/**
 * Order status enum representing the lifecycle states of an order.
 * Defines valid state transitions and business rules.
 */
public enum OrderStatus {
    /**
     * Order is being prepared (initial state)
     */
    PREPARING,
    
    /**
     * Order is pending confirmation/payment
     */
    PENDING,
    
    /**
     * Order has been confirmed and items reserved
     */
    CONFIRMED,
    
    /**
     * Order has been shipped to customer
     */
    SHIPPED,
    
    /**
     * Order has been delivered to customer (final state)
     */
    DELIVERED,
    
    /**
     * Order has been cancelled
     */
    CANCELLED;
    
    /**
     * Checks if an order in this status can be cancelled.
     * Only PREPARING, PENDING, and CONFIRMED orders can be cancelled.
     * 
     * @return true if cancellable, false otherwise
     */
    public boolean canBeCancelled() {
        return this == PREPARING || this == PENDING || this == CONFIRMED;
    }
    
    /**
     * Checks if this status represents a final state.
     * Final states: DELIVERED, CANCELLED
     * 
     * @return true if final state
     */
    public boolean isFinalState() {
        return this == DELIVERED || this == CANCELLED;
    }
}

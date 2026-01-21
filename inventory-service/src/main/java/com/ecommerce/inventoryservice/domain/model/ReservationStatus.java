package com.ecommerce.inventoryservice.domain.model;

/**
 * Enumeration representing the lifecycle states of a stock reservation.
 * Follows state machine: PENDING → CONFIRMED | CANCELLED | EXPIRED
 */
public enum ReservationStatus {
    /**
     * Initial state - reservation created and stock is reserved
     */
    PENDING,
    
    /**
     * Reservation confirmed - order payment successful
     */
    CONFIRMED,
    
    /**
     * Reservation cancelled - order cancelled, stock returned
     */
    CANCELLED,
    
    /**
     * Reservation expired due to TTL timeout
     */
    EXPIRED;

    /**
     * Check if this status represents a terminal state.
     * Terminal states cannot transition to other states.
     *
     * @return true if status is CONFIRMED, CANCELLED, or EXPIRED
     */
    public boolean isTerminal() {
        return this == CONFIRMED || this == CANCELLED || this == EXPIRED;
    }
}

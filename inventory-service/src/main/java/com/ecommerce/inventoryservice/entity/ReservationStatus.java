package com.ecommerce.inventoryservice.entity;

/**
 * Enumeration representing the lifecycle states of a stock reservation
 */
public enum ReservationStatus {
    /**
     * Reservation is currently active and stock is reserved
     */
    ACTIVE,
    
    /**
     * Reservation was explicitly released and stock returned to available pool
     */
    RELEASED,
    
    /**
     * Reservation expired due to TTL timeout and is no longer valid
     */
    EXPIRED
}

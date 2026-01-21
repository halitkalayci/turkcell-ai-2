package com.ecommerce.inventoryservice.exception;

/**
 * Exception thrown when a reservation operation fails due to concurrent modifications
 * This occurs when optimistic locking detects a conflict
 * Results in HTTP 409 Conflict response
 */
public class ReservationConflictException extends RuntimeException {

    public ReservationConflictException() {
        super("Stock levels changed during reservation. Please retry the operation.");
    }

    public ReservationConflictException(String message) {
        super(message);
    }
}

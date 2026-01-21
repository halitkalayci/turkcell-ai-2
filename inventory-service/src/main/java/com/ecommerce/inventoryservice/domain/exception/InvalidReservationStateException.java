package com.ecommerce.inventoryservice.domain.exception;

/**
 * Exception thrown when attempting an invalid state transition on a reservation.
 * For example: confirming an expired reservation, cancelling a confirmed reservation.
 */
public class InvalidReservationStateException extends RuntimeException {

    public InvalidReservationStateException(String message) {
        super(message);
    }
}

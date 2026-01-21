package com.ecommerce.inventoryservice.exception;

import java.util.UUID;

/**
 * Exception thrown when a requested reservation is not found or has expired
 * Results in HTTP 404 Not Found response
 */
public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(UUID reservationId) {
        super(String.format("Reservation with ID %s does not exist or has expired", reservationId));
    }
}

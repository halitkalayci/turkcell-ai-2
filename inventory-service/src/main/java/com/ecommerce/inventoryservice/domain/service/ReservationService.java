package com.ecommerce.inventoryservice.domain.service;

import com.ecommerce.inventoryservice.domain.exception.InvalidReservationStateException;
import com.ecommerce.inventoryservice.domain.model.Reservation;
import com.ecommerce.inventoryservice.domain.model.ReservationItem;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain service for reservation lifecycle management.
 * Stateless - contains business logic for reservation operations.
 * NO repository dependencies - works with domain models passed as parameters.
 */
@Component("domainReservationService")
public class ReservationService {

    /**
     * Creates a new reservation in PENDING status.
     *
     * @param orderId the order this reservation belongs to
     * @param items list of items to reserve
     * @param createdAt creation timestamp
     * @param expiresAt expiration timestamp
     * @return new reservation with generated ID
     */
    public Reservation createReservation(UUID orderId, List<ReservationItem> items, 
                                        Instant createdAt, Instant expiresAt) {
        UUID reservationId = UUID.randomUUID();
        return new Reservation(reservationId, orderId, items, createdAt, expiresAt);
    }

    /**
     * Validates that a reservation can be cancelled.
     *
     * @param reservation the reservation to validate
     * @throws InvalidReservationStateException if cannot be cancelled
     */
    public void validateReservationCancellation(Reservation reservation) {
        if (!reservation.canBeCancelled()) {
            throw new InvalidReservationStateException(
                String.format("Cannot cancel reservation %s in %s state",
                    reservation.getId(),
                    reservation.getStatus())
            );
        }
    }

    /**
     * Validates that a reservation can be confirmed.
     *
     * @param reservation the reservation to validate
     * @throws InvalidReservationStateException if cannot be confirmed or is expired
     */
    public void validateReservationConfirmation(Reservation reservation) {
        if (!reservation.canBeConfirmed()) {
            if (reservation.isExpired()) {
                throw new InvalidReservationStateException(
                    String.format("Cannot confirm expired reservation %s", 
                        reservation.getId())
                );
            } else {
                throw new InvalidReservationStateException(
                    String.format("Cannot confirm reservation %s in %s state",
                        reservation.getId(),
                        reservation.getStatus())
                );
            }
        }
    }
}

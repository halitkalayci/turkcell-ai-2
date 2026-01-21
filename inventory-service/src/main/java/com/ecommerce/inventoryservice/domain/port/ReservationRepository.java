package com.ecommerce.inventoryservice.domain.port;

import com.ecommerce.inventoryservice.domain.model.Reservation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for reservation persistence operations.
 * Defines the contract that infrastructure adapters must implement.
 * Works with domain models (NOT JPA entities).
 */
public interface ReservationRepository {

    /**
     * Saves a reservation (create or update).
     *
     * @param reservation the reservation to save
     * @return the saved reservation
     */
    Reservation save(Reservation reservation);

    /**
     * Finds a reservation by its unique ID.
     *
     * @param id the reservation identifier
     * @return Optional containing the reservation if found, empty otherwise
     */
    Optional<Reservation> findById(UUID id);

    /**
     * Finds a reservation by order ID.
     *
     * @param orderId the order identifier
     * @return Optional containing the reservation if found, empty otherwise
     */
    Optional<Reservation> findByOrderId(UUID orderId);

    /**
     * Finds all reservations that have expired as of the given time.
     * Used by background jobs to clean up expired reservations.
     *
     * @param currentTime the time to check against
     * @return list of expired reservations (may be empty)
     */
    List<Reservation> findExpiredReservations(Instant currentTime);
}

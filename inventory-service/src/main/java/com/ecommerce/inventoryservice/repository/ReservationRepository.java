package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Reservation entity
 * Provides CRUD operations and custom queries for reservation management
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    /**
     * Finds a reservation by its unique reservation ID
     *
     * @param reservationId the UUID of the reservation
     * @return Optional containing the reservation if found, empty otherwise
     */
    Optional<Reservation> findByReservationId(UUID reservationId);
}

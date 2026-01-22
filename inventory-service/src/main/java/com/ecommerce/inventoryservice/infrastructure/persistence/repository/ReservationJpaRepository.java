package com.ecommerce.inventoryservice.infrastructure.persistence.repository;

import com.ecommerce.inventoryservice.infrastructure.persistence.entity.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for reservation persistence.
 * Works with ReservationEntity (NOT domain model).
 */
public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, UUID> {

    /**
     * Finds a reservation by order ID.
     *
     * @param orderId the order identifier
     * @return Optional containing the reservation entity if found
     */
    Optional<ReservationEntity> findByOrderId(UUID orderId);

    /**
     * Finds all reservations that have expired and are still PENDING.
     * Used by background jobs to clean up expired reservations.
     *
     * @param currentTime the time to check against
     * @return list of expired reservation entities
     */
    @Query("SELECT r FROM ReservationEntity r WHERE r.expiresAt < :currentTime " +
           "AND r.status = 'PENDING'")
    List<ReservationEntity> findExpiredReservations(@Param("currentTime") Instant currentTime);
}

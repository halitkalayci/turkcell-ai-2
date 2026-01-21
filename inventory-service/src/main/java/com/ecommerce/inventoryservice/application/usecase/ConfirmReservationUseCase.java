package com.ecommerce.inventoryservice.application.usecase;

import com.ecommerce.inventoryservice.domain.exception.ReservationNotFoundException;
import com.ecommerce.inventoryservice.domain.model.Reservation;
import com.ecommerce.inventoryservice.domain.port.ReservationRepository;
import com.ecommerce.inventoryservice.domain.service.ReservationService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use-case: Confirm a reservation (order payment successful).
 * Transactional write operation.
 * 
 * Orchestrates:
 * 1. Load reservation from repository
 * 2. Validate reservation can be confirmed (domain service)
 * 3. Confirm reservation (domain method with state transition)
 * 4. Persist updated reservation
 */
public class ConfirmReservationUseCase {

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    public ConfirmReservationUseCase(ReservationRepository reservationRepository,
                                    ReservationService reservationService) {
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
    }

    /**
     * Confirms a reservation.
     *
     * @param reservationId the reservation identifier
     * @return the confirmed reservation
     * @throws ReservationNotFoundException if reservation doesn't exist
     * @throws com.ecommerce.inventoryservice.domain.exception.InvalidReservationStateException if cannot be confirmed
     */
    @Transactional
    public Reservation execute(UUID reservationId) {
        // 1. Load reservation
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        // 2. Validate can be confirmed (domain service validates business rules)
        reservationService.validateReservationConfirmation(reservation);

        // 3. Confirm reservation (domain method handles state transition)
        reservation.confirm();

        // 4. Persist and return
        return reservationRepository.save(reservation);
    }
}

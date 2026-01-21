package com.ecommerce.inventoryservice.application.usecase;

import com.ecommerce.inventoryservice.domain.exception.ReservationNotFoundException;
import com.ecommerce.inventoryservice.domain.model.Reservation;
import com.ecommerce.inventoryservice.domain.port.ReservationRepository;

import java.util.UUID;

/**
 * Use-case: Retrieve reservation by ID.
 * Read-only operation - no transaction required.
 */
public class GetReservationUseCase {

    private final ReservationRepository reservationRepository;

    public GetReservationUseCase(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    /**
     * Retrieves reservation by ID.
     *
     * @param reservationId the reservation identifier
     * @return the reservation
     * @throws ReservationNotFoundException if reservation doesn't exist
     */
    public Reservation execute(UUID reservationId) {
        return reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    }
}

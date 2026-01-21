package com.ecommerce.inventoryservice.application.usecase;

import com.ecommerce.inventoryservice.domain.exception.ReservationNotFoundException;
import com.ecommerce.inventoryservice.domain.model.InventoryItem;
import com.ecommerce.inventoryservice.domain.model.Reservation;
import com.ecommerce.inventoryservice.domain.model.ReservationItem;
import com.ecommerce.inventoryservice.domain.port.InventoryRepository;
import com.ecommerce.inventoryservice.domain.port.ReservationRepository;
import com.ecommerce.inventoryservice.domain.service.ReservationService;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use-case: Cancel a reservation and release reserved stock back to available pool.
 * Transactional write operation - handles complex orchestration.
 * 
 * Orchestrates:
 * 1. Load reservation from repository
 * 2. Validate reservation can be cancelled (domain service)
 * 3. Load inventory items for all reserved products
 * 4. Release reserved quantities (domain logic)
 * 5. Save updated inventory items
 * 6. Cancel reservation (domain method with state transition)
 * 7. Persist updated reservation
 * 
 * Transaction boundary ensures atomicity of stock release and reservation cancellation.
 */
public class CancelReservationUseCase {

    private final ReservationRepository reservationRepository;
    private final InventoryRepository inventoryRepository;
    private final ReservationService reservationService;

    public CancelReservationUseCase(ReservationRepository reservationRepository,
                                   InventoryRepository inventoryRepository,
                                   ReservationService reservationService) {
        this.reservationRepository = reservationRepository;
        this.inventoryRepository = inventoryRepository;
        this.reservationService = reservationService;
    }

    /**
     * Cancels a reservation and releases stock.
     *
     * @param reservationId the reservation identifier
     * @return the cancelled reservation
     * @throws ReservationNotFoundException if reservation doesn't exist
     * @throws com.ecommerce.inventoryservice.domain.exception.InvalidReservationStateException if cannot be cancelled
     */
    @Transactional
    public Reservation execute(UUID reservationId) {
        // 1. Load reservation
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        // 2. Validate can be cancelled (domain service validates business rules)
        reservationService.validateReservationCancellation(reservation);

        // 3. Extract product IDs from reservation items
        List<UUID> productIds = reservation.getItems().stream()
            .map(ReservationItem::getProductId)
            .toList();

        // 4. Load inventory items
        List<InventoryItem> inventoryItems = inventoryRepository.findByProductIds(productIds);

        // 5. Build inventory map for efficient lookup
        Map<UUID, InventoryItem> inventoryMap = inventoryItems.stream()
            .collect(Collectors.toMap(InventoryItem::getProductId, item -> item));

        // 6. Release reserved quantities back to available using domain logic
        for (ReservationItem item : reservation.getItems()) {
            InventoryItem inventoryItem = inventoryMap.get(item.getProductId());
            if (inventoryItem != null) {
                inventoryItem.release(item.getQuantity());
            }
            // If inventory item not found, it means data inconsistency - transaction will rollback
        }

        // 7. Save updated inventory items
        inventoryRepository.saveAll(new ArrayList<>(inventoryMap.values()));

        // 8. Cancel reservation (domain method handles state transition)
        reservation.cancel();

        // 9. Persist and return
        return reservationRepository.save(reservation);
    }
}

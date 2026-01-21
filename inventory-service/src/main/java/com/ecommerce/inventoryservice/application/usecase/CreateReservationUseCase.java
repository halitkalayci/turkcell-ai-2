package com.ecommerce.inventoryservice.application.usecase;

import com.ecommerce.inventoryservice.domain.exception.ReservationConflictException;
import com.ecommerce.inventoryservice.domain.model.InventoryItem;
import com.ecommerce.inventoryservice.domain.model.Reservation;
import com.ecommerce.inventoryservice.domain.model.ReservationItem;
import com.ecommerce.inventoryservice.domain.port.InventoryRepository;
import com.ecommerce.inventoryservice.domain.port.ReservationRepository;
import com.ecommerce.inventoryservice.domain.service.ReservationService;
import com.ecommerce.inventoryservice.domain.service.StockService;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use-case: Create a stock reservation for an order.
 * Transactional write operation - handles complex orchestration.
 * 
 * Orchestrates:
 * 1. Validate no duplicate reservation exists
 * 2. Load inventory items
 * 3. Validate stock availability (domain service)
 * 4. Reserve stock in inventory items (domain logic)
 * 5. Save updated inventory
 * 6. Create reservation domain object
 * 7. Persist reservation
 * 
 * Transaction boundary ensures atomicity of all operations.
 */
public class CreateReservationUseCase {

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final StockService stockService;
    private final ReservationService reservationService;

    public CreateReservationUseCase(InventoryRepository inventoryRepository,
                                   ReservationRepository reservationRepository,
                                   StockService stockService,
                                   ReservationService reservationService) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.stockService = stockService;
        this.reservationService = reservationService;
    }

    /**
     * Creates a new stock reservation.
     *
     * @param orderId the order ID to reserve stock for
     * @param items list of items to reserve
     * @return the created reservation
     * @throws ReservationConflictException if reservation already exists or concurrent conflict
     * @throws com.ecommerce.inventoryservice.domain.exception.ProductNotFoundException if product not found
     * @throws com.ecommerce.inventoryservice.domain.exception.InsufficientStockException if insufficient stock
     */
    @Transactional
    public Reservation execute(UUID orderId, List<ReservationItem> items) {
        // 1. Validate no duplicate reservation for this order
        reservationRepository.findByOrderId(orderId).ifPresent(existing -> {
            throw new ReservationConflictException(
                "Reservation already exists for order: " + orderId
            );
        });

        // 2. Extract product IDs and load inventory items
        List<UUID> productIds = items.stream()
            .map(ReservationItem::getProductId)
            .toList();
        
        List<InventoryItem> inventoryItems = inventoryRepository.findByProductIds(productIds);
        
        // 3. Build inventory map for efficient lookup
        Map<UUID, InventoryItem> inventoryMap = inventoryItems.stream()
            .collect(Collectors.toMap(InventoryItem::getProductId, item -> item));

        // 4. Validate stock availability using domain service
        // Throws exception if any product not found or insufficient stock
        stockService.validateStockReservation(items, inventoryMap);

        // 5. Reserve stock in each inventory item using domain logic
        for (ReservationItem item : items) {
            InventoryItem inventoryItem = inventoryMap.get(item.getProductId());
            inventoryItem.reserve(item.getQuantity());
        }

        // 6. Save updated inventory items (optimistic locking applies here)
        inventoryRepository.saveAll(new ArrayList<>(inventoryMap.values()));

        // 7. Create reservation domain object using domain service
        Instant now = Instant.now();
        Instant expiresAt = stockService.calculateReservationExpiry(now);
        Reservation reservation = reservationService.createReservation(
            orderId, 
            items, 
            now, 
            expiresAt
        );

        // 8. Persist and return reservation
        return reservationRepository.save(reservation);
    }

    /**
     * Overloaded method with custom TTL.
     *
     * @param orderId the order ID
     * @param items items to reserve
     * @param ttlMinutes custom time-to-live in minutes
     * @return the created reservation
     */
    @Transactional
    public Reservation execute(UUID orderId, List<ReservationItem> items, int ttlMinutes) {
        // Same logic but with custom TTL
        reservationRepository.findByOrderId(orderId).ifPresent(existing -> {
            throw new ReservationConflictException(
                "Reservation already exists for order: " + orderId
            );
        });

        List<UUID> productIds = items.stream()
            .map(ReservationItem::getProductId)
            .toList();
        
        List<InventoryItem> inventoryItems = inventoryRepository.findByProductIds(productIds);
        Map<UUID, InventoryItem> inventoryMap = inventoryItems.stream()
            .collect(Collectors.toMap(InventoryItem::getProductId, item -> item));

        stockService.validateStockReservation(items, inventoryMap);

        for (ReservationItem item : items) {
            InventoryItem inventoryItem = inventoryMap.get(item.getProductId());
            inventoryItem.reserve(item.getQuantity());
        }

        inventoryRepository.saveAll(new ArrayList<>(inventoryMap.values()));

        Instant now = Instant.now();
        Instant expiresAt = stockService.calculateReservationExpiry(now, ttlMinutes);
        Reservation reservation = reservationService.createReservation(
            orderId, 
            items, 
            now, 
            expiresAt
        );

        return reservationRepository.save(reservation);
    }
}

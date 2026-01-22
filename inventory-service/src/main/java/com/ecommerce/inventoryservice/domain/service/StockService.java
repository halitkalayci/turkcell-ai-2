package com.ecommerce.inventoryservice.domain.service;

import com.ecommerce.inventoryservice.application.dto.ProductQuantity;
import com.ecommerce.inventoryservice.domain.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.domain.exception.ProductNotFoundException;
import com.ecommerce.inventoryservice.domain.model.InventoryItem;
import com.ecommerce.inventoryservice.domain.model.ReservationItem;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Domain service for complex stock validation logic.
 * Stateless - contains business logic that doesn't naturally fit in a single entity.
 * NO repository dependencies - works with domain models passed as parameters.
 */
@Component
public class StockService {

    private static final int DEFAULT_RESERVATION_TTL_MINUTES = 15;

    /**
     * Validates that all requested items can be reserved.
     * Checks product existence and stock availability for each item.
     *
     * @param items list of items to reserve
     * @param inventory map of product IDs to their inventory items
     * @throws ProductNotFoundException if any product is not found
     * @throws InsufficientStockException if any product has insufficient stock
     */
    public void validateStockReservation(List<ReservationItem> items, 
                                         Map<UUID, InventoryItem> inventory) {
        List<ProductQuantity> insufficientItems = new ArrayList<>();
        
        for (ReservationItem item : items) {
            InventoryItem inventoryItem = inventory.get(item.getProductId());
            
            if (inventoryItem == null) {
                throw new ProductNotFoundException(item.getProductId());
            }
            
            if (!inventoryItem.isAvailable(item.getQuantity())) {
                insufficientItems.add(new ProductQuantity(
                    item.getProductId(),
                    item.getQuantity(),
                    inventoryItem.getAvailableQuantity()
                ));
            }
        }
        
        if (!insufficientItems.isEmpty()) {
            throw new InsufficientStockException(
                "Cannot reserve requested quantities due to insufficient stock",
                insufficientItems
            );
        }
    }

    /**
     * Calculates when a reservation should expire.
     * Default: current time + 15 minutes
     *
     * @param currentTime the starting time
     * @return expiration instant
     */
    public Instant calculateReservationExpiry(Instant currentTime) {
        return currentTime.plus(DEFAULT_RESERVATION_TTL_MINUTES, ChronoUnit.MINUTES);
    }

    /**
     * Calculates reservation expiry with custom TTL.
     *
     * @param currentTime the starting time
     * @param ttlMinutes time-to-live in minutes
     * @return expiration instant
     */
    public Instant calculateReservationExpiry(Instant currentTime, int ttlMinutes) {
        if (ttlMinutes <= 0) {
            throw new IllegalArgumentException("TTL must be positive: " + ttlMinutes);
        }
        return currentTime.plus(ttlMinutes, ChronoUnit.MINUTES);
    }

    /**
     * Checks if multiple products have sufficient availability.
     *
     * @param productQuantities map of product IDs to requested quantities
     * @param items list of inventory items to check against
     * @return true if ALL products have sufficient stock
     */
    public boolean checkMultipleAvailability(Map<UUID, Integer> productQuantities, 
                                            List<InventoryItem> items) {
        Map<UUID, InventoryItem> inventoryMap = items.stream()
            .collect(java.util.stream.Collectors.toMap(
                InventoryItem::getProductId,
                item -> item
            ));
        
        for (Map.Entry<UUID, Integer> entry : productQuantities.entrySet()) {
            UUID productId = entry.getKey();
            Integer requestedQuantity = entry.getValue();
            
            InventoryItem item = inventoryMap.get(productId);
            if (item == null || !item.isAvailable(requestedQuantity)) {
                return false;
            }
        }
        
        return true;
    }
}

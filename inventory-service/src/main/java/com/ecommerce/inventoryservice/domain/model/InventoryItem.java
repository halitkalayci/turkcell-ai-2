package com.ecommerce.inventoryservice.domain.model;

import com.ecommerce.inventoryservice.domain.exception.InsufficientStockException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain model representing a product's inventory stock levels.
 * Pure business logic with NO framework dependencies.
 * 
 * Invariant: totalQuantity = availableQuantity + reservedQuantity (ALWAYS)
 */
public class InventoryItem {

    private final UUID productId;
    private int availableQuantity;
    private int reservedQuantity;
    private int totalQuantity;
    private Instant lastUpdatedAt;
    private long version;

    /**
     * Creates a new inventory item with validation.
     *
     * @param productId unique product identifier
     * @param availableQuantity quantity available for reservation
     * @param reservedQuantity quantity currently reserved
     * @param version optimistic lock version
     */
    public InventoryItem(UUID productId, int availableQuantity, int reservedQuantity, long version) {
        validateNotNull(productId, "Product ID cannot be null");
        validateNonNegative(availableQuantity, "Available quantity");
        validateNonNegative(reservedQuantity, "Reserved quantity");
        
        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
        this.version = version;
        this.lastUpdatedAt = Instant.now();
        
        recalculateTotalQuantity();
        validateInvariant();
    }

    /**
     * Checks if requested quantity is available for reservation.
     *
     * @param requestedQuantity the quantity to check
     * @return true if sufficient stock is available
     */
    public boolean isAvailable(int requestedQuantity) {
        validateNonNegative(requestedQuantity, "Requested quantity");
        return this.availableQuantity >= requestedQuantity;
    }

    /**
     * Reserves the specified quantity of stock.
     * Decreases available, increases reserved.
     *
     * @param quantity the quantity to reserve (must be positive)
     * @throws InsufficientStockException if not enough stock available
     * @throws IllegalArgumentException if quantity is invalid
     */
    public void reserve(int quantity) {
        validatePositive(quantity, "Reserve quantity");
        
        if (!isAvailable(quantity)) {
            throw new InsufficientStockException(
                String.format("Cannot reserve %d units of product %s. Only %d available.", 
                    quantity, productId, availableQuantity),
                null
            );
        }
        
        availableQuantity -= quantity;
        reservedQuantity += quantity;
        lastUpdatedAt = Instant.now();
        
        recalculateTotalQuantity();
        validateInvariant();
    }

    /**
     * Releases reserved stock back to available pool.
     * Increases available, decreases reserved.
     *
     * @param quantity the quantity to release (must be positive)
     * @throws IllegalArgumentException if quantity invalid or exceeds reserved
     */
    public void release(int quantity) {
        validatePositive(quantity, "Release quantity");
        
        if (quantity > reservedQuantity) {
            throw new IllegalArgumentException(
                String.format("Cannot release %d units. Only %d units are reserved.", 
                    quantity, reservedQuantity)
            );
        }
        
        reservedQuantity -= quantity;
        availableQuantity += quantity;
        lastUpdatedAt = Instant.now();
        
        recalculateTotalQuantity();
        validateInvariant();
    }

    /**
     * Adds new stock to inventory.
     * Increases available and total quantities.
     *
     * @param quantity the quantity to add (must be positive)
     */
    public void restock(int quantity) {
        validatePositive(quantity, "Restock quantity");
        
        availableQuantity += quantity;
        lastUpdatedAt = Instant.now();
        
        recalculateTotalQuantity();
        validateInvariant();
    }

    /**
     * Recalculates total quantity from available and reserved.
     * Maintains the invariant: total = available + reserved
     */
    private void recalculateTotalQuantity() {
        this.totalQuantity = this.availableQuantity + this.reservedQuantity;
    }

    /**
     * Validates the domain invariant.
     * 
     * @throws IllegalStateException if invariant is violated
     */
    private void validateInvariant() {
        if (availableQuantity < 0 || reservedQuantity < 0) {
            throw new IllegalStateException(
                String.format("Invalid inventory state for product %s: available=%d, reserved=%d", 
                    productId, availableQuantity, reservedQuantity)
            );
        }
        
        int expectedTotal = availableQuantity + reservedQuantity;
        if (totalQuantity != expectedTotal) {
            throw new IllegalStateException(
                String.format("Invariant violation: total=%d, but available+reserved=%d", 
                    totalQuantity, expectedTotal)
            );
        }
    }

    private void validateNotNull(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative: " + value);
        }
    }

    private void validatePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive: " + value);
        }
    }

    // Getters only (no setters for controlled state changes)
    
    public UUID getProductId() {
        return productId;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryItem that = (InventoryItem) o;
        return Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return "InventoryItem{" +
                "productId=" + productId +
                ", availableQuantity=" + availableQuantity +
                ", reservedQuantity=" + reservedQuantity +
                ", totalQuantity=" + totalQuantity +
                ", version=" + version +
                '}';
    }
}

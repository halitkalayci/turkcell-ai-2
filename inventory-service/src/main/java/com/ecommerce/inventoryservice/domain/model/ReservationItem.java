package com.ecommerce.inventoryservice.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a single item within a reservation.
 * Immutable - once created, values cannot be changed.
 */
public class ReservationItem {
    
    private final UUID productId;
    private final int quantity;

    /**
     * Creates a new reservation item with validation.
     *
     * @param productId the product identifier (must not be null)
     * @param quantity the reserved quantity (must be positive)
     * @throws IllegalArgumentException if validation fails
     */
    public ReservationItem(UUID productId, int quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive, got: " + quantity);
        }
        
        this.productId = productId;
        this.quantity = quantity;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    /**
     * Value objects are equal if all fields are equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReservationItem that = (ReservationItem) o;
        return quantity == that.quantity && 
               Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, quantity);
    }

    @Override
    public String toString() {
        return "ReservationItem{" +
                "productId=" + productId +
                ", quantity=" + quantity +
                '}';
    }
}

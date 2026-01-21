package com.ecommerce.inventoryservice.application.dto;

import java.util.UUID;

/**
 * Input DTO for availability check requests.
 * Represents a product and the requested quantity.
 */
public record ProductQuantity(UUID productId, int quantity) {
    
    /**
     * Compact constructor with validation.
     */
    public ProductQuantity {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive, got: " + quantity);
        }
    }
}

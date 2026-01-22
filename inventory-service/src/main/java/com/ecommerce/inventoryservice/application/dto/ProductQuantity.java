package com.ecommerce.inventoryservice.application.dto;

import java.util.UUID;

/**
 * Input/Output DTO for product quantity operations.
 * Can represent requested quantity or include available quantity for error reporting.
 */
public record ProductQuantity(UUID productId, int requestedQuantity, int availableQuantity) {
    
    /**
     * Constructor for requests (available is unknown).
     */
    public ProductQuantity(UUID productId, int requestedQuantity) {
        this(productId, requestedQuantity, 0);
    }
    
    /**
     * Compact constructor with validation.
     */
    public ProductQuantity {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("Requested quantity must be positive, got: " + requestedQuantity);
        }
    }
    
    /**
     * Convenience getter for quantity (backwards compatibility).
     */
    public int quantity() {
        return requestedQuantity;
    }
}


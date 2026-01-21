package com.ecommerce.inventoryservice.application.dto;

import java.util.List;

/**
 * Result DTO for availability check operations.
 * Contains overall availability status and per-item details.
 */
public record AvailabilityResult(
    boolean allAvailable,
    List<ItemAvailability> items
) {
    public AvailabilityResult {
        if (items == null) {
            throw new IllegalArgumentException("Items list cannot be null");
        }
    }
}

package com.ecommerce.inventoryservice.application.dto;

import java.util.UUID;

/**
 * Per-item availability information.
 * Used in availability check results to show detailed status for each product.
 */
public record ItemAvailability(
    UUID productId,
    boolean available,
    int requestedQuantity,
    int availableQuantity
) {}

package com.ecommerce.orderservice.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Application-level DTO for order items.
 * Used for communication between use-cases.
 */
public record OrderItemDto(
    UUID productId,
    String productName,
    int quantity,
    BigDecimal unitPrice
) {
    public OrderItemDto {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit price must be positive");
        }
    }
}

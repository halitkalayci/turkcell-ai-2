package com.ecommerce.inventoryservice.domain.exception;

import java.util.UUID;

/**
 * Exception thrown when a requested product is not found in inventory.
 * Results in HTTP 404 Not Found response.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(UUID productId) {
        super(String.format("Product with ID %s does not exist in inventory", productId));
    }
}

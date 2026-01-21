package com.ecommerce.inventoryservice.application.usecase;

import com.ecommerce.inventoryservice.domain.exception.ProductNotFoundException;
import com.ecommerce.inventoryservice.domain.model.InventoryItem;
import com.ecommerce.inventoryservice.domain.port.InventoryRepository;

import java.util.UUID;

/**
 * Use-case: Retrieve inventory information for a specific product.
 * Read-only operation - no transaction required.
 */
public class GetInventoryItemUseCase {

    private final InventoryRepository inventoryRepository;

    public GetInventoryItemUseCase(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Retrieves inventory item by product ID.
     *
     * @param productId the product identifier
     * @return the inventory item with current stock levels
     * @throws ProductNotFoundException if product doesn't exist
     */
    public InventoryItem execute(UUID productId) {
        return inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}

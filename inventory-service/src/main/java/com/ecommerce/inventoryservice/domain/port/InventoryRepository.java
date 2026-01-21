package com.ecommerce.inventoryservice.domain.port;

import com.ecommerce.inventoryservice.domain.model.InventoryItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for inventory persistence operations.
 * Defines the contract that infrastructure adapters must implement.
 * Works with domain models (NOT JPA entities).
 */
public interface InventoryRepository {

    /**
     * Finds an inventory item by product ID.
     *
     * @param productId the product identifier
     * @return Optional containing the item if found, empty otherwise
     */
    Optional<InventoryItem> findByProductId(UUID productId);

    /**
     * Finds multiple inventory items by product IDs.
     *
     * @param productIds list of product identifiers
     * @return list of found items (may be empty or partial)
     */
    List<InventoryItem> findByProductIds(List<UUID> productIds);

    /**
     * Saves an inventory item (create or update).
     *
     * @param item the inventory item to save
     * @return the saved item with updated version
     */
    InventoryItem save(InventoryItem item);

    /**
     * Saves multiple inventory items in a batch.
     *
     * @param items list of inventory items to save
     */
    void saveAll(List<InventoryItem> items);
}

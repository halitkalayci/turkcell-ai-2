package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for InventoryItem entity
 * Provides CRUD operations and custom queries for inventory management
 */
@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    /**
     * Finds an inventory item by product ID
     *
     * @param productId the UUID of the product
     * @return Optional containing the inventory item if found, empty otherwise
     */
    Optional<InventoryItem> findByProductId(UUID productId);
}

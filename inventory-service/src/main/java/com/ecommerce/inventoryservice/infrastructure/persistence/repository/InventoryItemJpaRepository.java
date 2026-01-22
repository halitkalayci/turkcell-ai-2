package com.ecommerce.inventoryservice.infrastructure.persistence.repository;

import com.ecommerce.inventoryservice.infrastructure.persistence.entity.InventoryItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for inventory item persistence.
 * Works with InventoryItemEntity (NOT domain model).
 */
public interface InventoryItemJpaRepository extends JpaRepository<InventoryItemEntity, UUID> {

    /**
     * Finds multiple inventory items by product IDs.
     *
     * @param productIds list of product identifiers
     * @return list of found inventory item entities
     */
    List<InventoryItemEntity> findByProductIdIn(List<UUID> productIds);
}

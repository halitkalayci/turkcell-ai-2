package com.ecommerce.inventoryservice.infrastructure.persistence.mapper;

import com.ecommerce.inventoryservice.domain.model.InventoryItem;
import com.ecommerce.inventoryservice.infrastructure.persistence.entity.InventoryItemEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for converting between InventoryItem domain model and InventoryItemEntity.
 * Pure data transfer - NO business logic.
 */
@Component
public class InventoryItemMapper {

    /**
     * Converts JPA entity to domain model.
     *
     * @param entity the JPA entity
     * @return the domain model
     */
    public InventoryItem toDomain(InventoryItemEntity entity) {
        if (entity == null) {
            return null;
        }

        return new InventoryItem(
            entity.getProductId(),
            entity.getAvailableQuantity(),
            entity.getReservedQuantity(),
            entity.getVersion()
        );
    }

    /**
     * Converts domain model to JPA entity.
     *
     * @param domain the domain model
     * @return the JPA entity
     */
    public InventoryItemEntity toEntity(InventoryItem domain) {
        if (domain == null) {
            return null;
        }

        return InventoryItemEntity.builder()
            .productId(domain.getProductId())
            .availableQuantity(domain.getAvailableQuantity())
            .reservedQuantity(domain.getReservedQuantity())
            .totalQuantity(domain.getTotalQuantity())
            .lastUpdatedAt(domain.getLastUpdatedAt())
            .version(domain.getVersion())
            .build();
    }

    /**
     * Converts list of entities to list of domain models.
     *
     * @param entities list of JPA entities
     * @return list of domain models
     */
    public List<InventoryItem> toDomainList(List<InventoryItemEntity> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
            .map(this::toDomain)
            .toList();
    }

    /**
     * Converts list of domain models to list of entities.
     *
     * @param domains list of domain models
     * @return list of JPA entities
     */
    public List<InventoryItemEntity> toEntityList(List<InventoryItem> domains) {
        if (domains == null) {
            return List.of();
        }

        return domains.stream()
            .map(this::toEntity)
            .toList();
    }
}

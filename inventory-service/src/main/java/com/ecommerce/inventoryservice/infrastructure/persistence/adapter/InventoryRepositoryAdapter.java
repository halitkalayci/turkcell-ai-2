package com.ecommerce.inventoryservice.infrastructure.persistence.adapter;

import com.ecommerce.inventoryservice.domain.model.InventoryItem;
import com.ecommerce.inventoryservice.domain.port.InventoryRepository;
import com.ecommerce.inventoryservice.infrastructure.persistence.entity.InventoryItemEntity;
import com.ecommerce.inventoryservice.infrastructure.persistence.mapper.InventoryItemMapper;
import com.ecommerce.inventoryservice.infrastructure.persistence.repository.InventoryItemJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementing InventoryRepository domain port.
 * Bridges domain layer with JPA persistence infrastructure.
 * 
 * Uses:
 * - InventoryItemJpaRepository for persistence operations
 * - InventoryItemMapper for domain ↔ entity conversion
 * 
 * Contains NO business logic - only coordinates persistence.
 */
@Component
@RequiredArgsConstructor
public class InventoryRepositoryAdapter implements InventoryRepository {

    private final InventoryItemJpaRepository jpaRepository;
    private final InventoryItemMapper mapper;

    @Override
    public Optional<InventoryItem> findByProductId(UUID productId) {
        return jpaRepository.findById(productId)
            .map(mapper::toDomain);
    }

    @Override
    public List<InventoryItem> findByProductIds(List<UUID> productIds) {
        List<InventoryItemEntity> entities = jpaRepository.findByProductIdIn(productIds);
        return mapper.toDomainList(entities);
    }

    @Override
    public InventoryItem save(InventoryItem item) {
        InventoryItemEntity entity = mapper.toEntity(item);
        InventoryItemEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public void saveAll(List<InventoryItem> items) {
        List<InventoryItemEntity> entities = mapper.toEntityList(items);
        jpaRepository.saveAll(entities);
    }
}

package com.ecommerce.inventoryservice.infrastructure.persistence.mapper;

import com.ecommerce.inventoryservice.domain.model.Reservation;
import com.ecommerce.inventoryservice.domain.model.ReservationItem;
import com.ecommerce.inventoryservice.infrastructure.persistence.entity.ReservationEntity;
import com.ecommerce.inventoryservice.infrastructure.persistence.entity.ReservationItemEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper for converting between Reservation domain model and ReservationEntity.
 * Handles nested ReservationItem mapping.
 * Pure data transfer - NO business logic.
 */
@Component
public class ReservationMapper {

    /**
     * Converts JPA entity to domain model.
     *
     * @param entity the JPA entity
     * @return the domain model
     */
    public Reservation toDomain(ReservationEntity entity) {
        if (entity == null) {
            return null;
        }

        // Map reservation items
        List<ReservationItem> items = entity.getItems().stream()
            .map(itemEntity -> new ReservationItem(
                itemEntity.getProductId(),
                itemEntity.getQuantity()
            ))
            .toList();

        // Reconstruct domain model with status (from persistence)
        return Reservation.reconstruct(
            entity.getId(),
            entity.getOrderId(),
            items,
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getExpiresAt()
        );
    }

    /**
     * Converts domain model to JPA entity.
     *
     * @param domain the domain model
     * @return the JPA entity
     */
    public ReservationEntity toEntity(Reservation domain) {
        if (domain == null) {
            return null;
        }

        // Create reservation entity
        ReservationEntity entity = ReservationEntity.builder()
            .id(domain.getId())
            .orderId(domain.getOrderId())
            .status(domain.getStatus())
            .createdAt(domain.getCreatedAt())
            .expiresAt(domain.getExpiresAt())
            .items(new ArrayList<>())
            .build();

        // Map reservation items and set bidirectional relationship
        List<ReservationItemEntity> itemEntities = domain.getItems().stream()
            .map(item -> ReservationItemEntity.builder()
                .reservation(entity)
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .build())
            .toList();

        entity.setItems(new ArrayList<>(itemEntities));

        return entity;
    }

    /**
     * Converts list of entities to list of domain models.
     *
     * @param entities list of JPA entities
     * @return list of domain models
     */
    public List<Reservation> toDomainList(List<ReservationEntity> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
            .map(this::toDomain)
            .toList();
    }
}

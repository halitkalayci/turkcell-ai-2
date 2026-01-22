package com.ecommerce.inventoryservice.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for inventory item persistence.
 * Pure persistence model - NO business logic.
 * All business operations are in domain.model.InventoryItem.
 */
@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItemEntity {

    @Id
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "available_quantity", nullable = false)
    @Builder.Default
    private Integer availableQuantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(name = "total_quantity", nullable = false)
    @Builder.Default
    private Integer totalQuantity = 0;

    @UpdateTimestamp
    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}

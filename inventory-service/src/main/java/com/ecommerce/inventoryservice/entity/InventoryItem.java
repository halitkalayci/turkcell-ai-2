package com.ecommerce.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a product's inventory stock levels
 * Tracks available, reserved, and total quantities with optimistic locking
 */
@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {

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
    private LocalDateTime lastUpdatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Checks if the requested quantity is available for reservation
     *
     * @param quantity the quantity to check
     * @return true if sufficient stock is available, false otherwise
     */
    public boolean isAvailable(int quantity) {
        return this.availableQuantity >= quantity;
    }

    /**
     * Business rule: totalQuantity = availableQuantity + reservedQuantity
     * This method ensures consistency when updating quantities
     */
    public void recalculateTotalQuantity() {
        this.totalQuantity = this.availableQuantity + this.reservedQuantity;
    }
}

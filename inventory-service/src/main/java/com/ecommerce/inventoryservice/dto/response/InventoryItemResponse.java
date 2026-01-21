package com.ecommerce.inventoryservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing stock information for a product
 * Maps to InventoryItemResponse schema in OpenAPI contract
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItemResponse {

    private UUID productId;

    private Integer availableQuantity;

    private Integer reservedQuantity;

    private Integer totalQuantity;

    private LocalDateTime lastUpdatedAt;
}

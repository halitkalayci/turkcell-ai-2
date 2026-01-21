package com.ecommerce.inventoryservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO representing an item with insufficient stock in error responses
 * Maps to InsufficientStockItem schema in OpenAPI contract
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsufficientStockItem {

    private UUID productId;

    private Integer requestedQuantity;

    private Integer availableQuantity;
}

package com.ecommerce.inventoryservice.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO representing availability details for a single product in the check response
 * Maps to AvailabilityCheckResultItem schema in OpenAPI contract
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityCheckResultItem {

    private UUID productId;

    private Integer requestedQuantity;

    private Integer availableQuantity;

    private Boolean sufficient;
}

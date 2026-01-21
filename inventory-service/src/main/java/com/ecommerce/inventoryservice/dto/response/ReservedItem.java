package com.ecommerce.inventoryservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO representing a single reserved item in a reservation response
 * Maps to ReservedItem schema in OpenAPI contract
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservedItem {

    private UUID productId;

    private Integer quantity;
}

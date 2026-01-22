package com.ecommerce.inventoryservice.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing the aggregated result of a stock availability check
 * Maps to AvailabilityCheckResponse schema in OpenAPI contract
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityCheckResponse {

    private Boolean available;

    private List<AvailabilityCheckResultItem> items;
}

package com.ecommerce.inventoryservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for checking stock availability across multiple products
 * Maps to AvailabilityCheckRequest schema in OpenAPI contract
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityCheckRequest {

    @NotNull(message = "Items list cannot be null")
    @Size(min = 1, message = "Request must contain at least one item")
    @Valid
    private List<AvailabilityCheckItem> items;
}

package com.ecommerce.inventoryservice.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for creating a stock reservation request
 * Maps to StockReservationRequest schema in OpenAPI contract
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservationRequest {

    @NotNull(message = "Order ID cannot be null")
    private UUID orderId;

    @NotNull(message = "Items list cannot be null")
    @Size(min = 1, message = "Request must contain at least one item")
    @Valid
    private List<ReservationItem> items;

    @Min(value = 1, message = "Reservation TTL must be at least 1 minute")
    @Max(value = 60, message = "Reservation TTL cannot exceed 60 minutes")
    @Builder.Default
    private Integer reservationTtlMinutes = 15;
}

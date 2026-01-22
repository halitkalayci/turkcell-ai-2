package com.ecommerce.inventoryservice.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing a stock reservation with all details
 * Maps to StockReservationResponse schema in OpenAPI contract
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservationResponse {

    private UUID reservationId;

    private UUID orderId;

    private String status;

    private List<ReservedItem> items;

    private Instant createdAt;

    private Instant expiresAt;
}

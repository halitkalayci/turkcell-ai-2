package com.ecommerce.orderservice.web.dto.response;

import com.ecommerce.orderservice.domain.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for cancel order response.
 * Maps to CancelOrderResponse schema in OpenAPI contract.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelOrderResponseDto {
    
    private UUID id;
    private OrderStatus status;
    private String message;
    private Instant cancelledAt;
}

package com.ecommerce.orderservice.web.dto.response;

import com.ecommerce.orderservice.domain.model.OrderStatus;
import com.ecommerce.orderservice.web.dto.request.AddressDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for order response.
 * Maps to OrderResponse schema in OpenAPI contract.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDto {
    
    private UUID id;
    private UUID customerId;
    private AddressDto address;
    private List<OrderItemResponseDto> items;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private Instant createdAt;
    private Instant updatedAt;
}

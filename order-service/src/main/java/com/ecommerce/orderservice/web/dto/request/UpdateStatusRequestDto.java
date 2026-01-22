package com.ecommerce.orderservice.web.dto.request;

import com.ecommerce.orderservice.domain.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for update order status request.
 * Maps to UpdateStatusRequest schema in OpenAPI contract.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStatusRequestDto {
    
    @NotNull(message = "Status is required")
    private OrderStatus status;
}

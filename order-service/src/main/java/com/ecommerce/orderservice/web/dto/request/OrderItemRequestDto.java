package com.ecommerce.orderservice.web.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for order item in create order request.
 * Maps to OrderItemRequest schema in OpenAPI contract.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequestDto {
    
    @NotNull(message = "Product ID is required")
    private UUID productId;
    
    @NotNull(message = "Product name is required")
    private String productName;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    @NotNull(message = "Unit price is required")
    @Min(value = 0, message = "Unit price must be positive")
    private BigDecimal unitPrice;
}

package com.ecommerce.orderservice.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for create order request.
 * Maps to OrderRequest schema in OpenAPI contract.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequestDto {
    
    @NotNull(message = "Customer ID is required")
    private UUID customerId;
    
    @NotNull(message = "Address is required")
    @Valid
    private AddressDto address;
    
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequestDto> items;
}

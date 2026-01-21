package com.ecommerce.orderservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {

    @NotNull(message = "Customer ID must not be null")
    private UUID customerId;

    @NotNull(message = "Address must not be null")
    @Valid
    private Address address;

    @NotEmpty(message = "Items list must not be empty")
    @Valid
    private List<OrderItemRequest> items;
}

package com.ecommerce.orderservice.dto.response;

import com.ecommerce.orderservice.dto.request.Address;
import com.ecommerce.orderservice.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private UUID id;
    private UUID customerId;
    private Address address;
    private List<OrderItemResponse> items;
    private OrderStatus status;
    private Double totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

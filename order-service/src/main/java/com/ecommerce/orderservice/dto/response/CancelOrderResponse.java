package com.ecommerce.orderservice.dto.response;

import com.ecommerce.orderservice.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelOrderResponse {

    private UUID id;
    private OrderStatus status;
    private String message;
    private LocalDateTime cancelledAt;
}

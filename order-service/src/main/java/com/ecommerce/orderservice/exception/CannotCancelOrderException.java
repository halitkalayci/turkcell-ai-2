package com.ecommerce.orderservice.exception;

import com.ecommerce.orderservice.entity.OrderStatus;

import java.util.UUID;

public class CannotCancelOrderException extends RuntimeException {

    private final UUID orderId;
    private final OrderStatus currentStatus;

    public CannotCancelOrderException(UUID orderId, OrderStatus currentStatus) {
        super(String.format("Order with ID %s cannot be cancelled because it has been %s", 
                orderId, currentStatus.name().toLowerCase()));
        this.orderId = orderId;
        this.currentStatus = currentStatus;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }
}

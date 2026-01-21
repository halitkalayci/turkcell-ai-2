package com.ecommerce.orderservice.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {
    
    public OrderNotFoundException(UUID orderId) {
        super(String.format("Order with ID %s not found", orderId));
    }
}

package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.domain.Order;
import com.ecommerce.orderservice.domain.OrderStatus;

import java.util.UUID;

public interface OrderService {
    
    Order createOrder(Order order);
    
    Order getOrderById(UUID orderId);
    
    Order cancelOrder(UUID orderId);
    
    Order updateOrderStatus(UUID orderId, OrderStatus newStatus);
}

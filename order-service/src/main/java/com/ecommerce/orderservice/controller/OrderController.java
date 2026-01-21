package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.api.OrdersApi;
import com.ecommerce.orderservice.api.model.*;
import com.ecommerce.orderservice.domain.Order;
import com.ecommerce.orderservice.mapper.OrderMapper;
import com.ecommerce.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class OrderController implements OrdersApi {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @Override
    public ResponseEntity<OrderResponse> createOrder(@Valid OrderRequest orderRequest) {
        log.info("Received request to create order for customer: {}", orderRequest.getCustomerId());

        Order order = orderMapper.toEntity(orderRequest);
        Order createdOrder = orderService.createOrder(order);
        OrderResponse response = orderMapper.toResponse(createdOrder);

        log.info("Order created successfully with ID: {}", createdOrder.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<OrderResponse> getOrderById(String id) {
        log.info("Received request to get order with ID: {}", id);

        UUID orderId = UUID.fromString(id);
        Order order = orderService.getOrderById(orderId);
        OrderResponse response = orderMapper.toResponse(order);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<CancelOrderResponse> cancelOrder(String id) {
        log.info("Received request to cancel order with ID: {}", id);

        UUID orderId = UUID.fromString(id);
        Order cancelledOrder = orderService.cancelOrder(orderId);
        CancelOrderResponse response = orderMapper.toCancelResponse(cancelledOrder);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<OrderResponse> updateOrderStatus(String id, @Valid UpdateStatusRequest updateStatusRequest) {
        log.info("Received request to update order {} status to {}", id, updateStatusRequest.getStatus());

        UUID orderId = UUID.fromString(id);
        com.ecommerce.orderservice.domain.OrderStatus newStatus = 
            orderMapper.toDomainStatus(updateStatusRequest.getStatus());
        
        Order updatedOrder = orderService.updateOrderStatus(orderId, newStatus);
        OrderResponse response = orderMapper.toResponse(updatedOrder);

        return ResponseEntity.ok(response);
    }
}

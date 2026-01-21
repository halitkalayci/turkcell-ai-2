package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.request.Address;
import com.ecommerce.orderservice.dto.request.OrderItemRequest;
import com.ecommerce.orderservice.dto.request.OrderRequest;
import com.ecommerce.orderservice.dto.request.UpdateStatusRequest;
import com.ecommerce.orderservice.dto.response.CancelOrderResponse;
import com.ecommerce.orderservice.dto.response.OrderItemResponse;
import com.ecommerce.orderservice.dto.response.OrderResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.exception.CannotCancelOrderException;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    // Mock prices - in real scenario, would fetch from product service
    private static final Double MOCK_UNIT_PRICE = 49.99;
    private static final String MOCK_PRODUCT_NAME = "Sample Product";

    @Override
    public OrderResponse createOrder(OrderRequest orderRequest) {
        log.info("Creating order for customer: {}", orderRequest.getCustomerId());

        // Build order entity from request
        Order order = Order.builder()
                .customerId(orderRequest.getCustomerId())
                .street(orderRequest.getAddress().getStreet())
                .city(orderRequest.getAddress().getCity())
                .state(orderRequest.getAddress().getState())
                .postalCode(orderRequest.getAddress().getPostalCode())
                .country(orderRequest.getAddress().getCountry())
                .status(OrderStatus.PREPARING)
                .build();

        // Add order items
        for (OrderItemRequest itemRequest : orderRequest.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .productName(MOCK_PRODUCT_NAME) // Would fetch from product service
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(MOCK_UNIT_PRICE) // Would fetch from product service
                    .build();
            
            orderItem.calculateTotalPrice();
            order.addItem(orderItem);
        }

        // Calculate total amount
        order.calculateTotalAmount();

        // Save order
        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {}", savedOrder.getId());

        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID id) {
        log.info("Fetching order with ID: {}", id);
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return mapToOrderResponse(order);
    }

    @Override
    public CancelOrderResponse cancelOrder(UUID id) {
        log.info("Attempting to cancel order with ID: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        // Check if order can be cancelled
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new CannotCancelOrderException(id, order.getStatus());
        }

        // Update order status to CANCELLED
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("Order {} cancelled successfully", id);

        return CancelOrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus())
                .message("Order has been successfully cancelled")
                .cancelledAt(order.getCancelledAt())
                .build();
    }

    @Override
    public OrderResponse updateOrderStatus(UUID id, UpdateStatusRequest request) {
        log.info("Updating status of order {} to {}", id, request.getStatus());

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        order.setStatus(request.getStatus());
        Order updatedOrder = orderRepository.save(order);

        log.info("Order {} status updated successfully", id);

        return mapToOrderResponse(updatedOrder);
    }

    /**
     * Maps Order entity to OrderResponse DTO
     */
    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());

        Address address = Address.builder()
                .street(order.getStreet())
                .city(order.getCity())
                .state(order.getState())
                .postalCode(order.getPostalCode())
                .country(order.getCountry())
                .build();

        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .address(address)
                .items(itemResponses)
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * Maps OrderItem entity to OrderItemResponse DTO
     */
    private OrderItemResponse mapToOrderItemResponse(OrderItem orderItem) {
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProductId())
                .productName(orderItem.getProductName())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .totalPrice(orderItem.getTotalPrice())
                .build();
    }
}

package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.domain.Order;
import com.ecommerce.orderservice.domain.OrderItem;
import com.ecommerce.orderservice.domain.OrderStatus;
import com.ecommerce.orderservice.exception.BusinessRuleViolationException;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    public Order createOrder(Order order) {
        log.debug("Creating order for customer: {}", order.getCustomerId());

        // Set default status
        order.setStatus(OrderStatus.PREPARING);

        // Process each order item
        for (OrderItem item : order.getItems()) {
            // For standalone service, mock product details
            item.setProductName("Product-" + item.getProductId());
            item.setUnitPrice(BigDecimal.valueOf(49.99)); // Mock price
            item.calculateTotalPrice();
            item.setOrder(order);
        }

        // Calculate total amount
        order.calculateTotalAmount();

        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {}", savedOrder.getId());

        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(UUID orderId) {
        log.debug("Fetching order with ID: {}", orderId);

        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Override
    public Order cancelOrder(UUID orderId) {
        log.debug("Cancelling order with ID: {}", orderId);

        Order order = getOrderById(orderId);

        if (!order.canBeCancelled()) {
            throw new BusinessRuleViolationException(
                    String.format("Order with ID %s cannot be cancelled because it has been shipped", orderId)
            );
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderRepository.save(order);

        log.info("Order {} cancelled successfully", orderId);
        return updatedOrder;
    }

    @Override
    public Order updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        log.debug("Updating order {} status to {}", orderId, newStatus);

        Order order = getOrderById(orderId);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleViolationException(
                    "Cannot update status of a cancelled order"
            );
        }

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        log.info("Order {} status updated to {}", orderId, newStatus);
        return updatedOrder;
    }
}

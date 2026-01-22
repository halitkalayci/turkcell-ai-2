package com.ecommerce.orderservice.application.usecase;

import com.ecommerce.orderservice.domain.exception.InvalidOrderStateException;
import com.ecommerce.orderservice.domain.exception.OrderNotFoundException;
import com.ecommerce.orderservice.domain.model.Order;
import com.ecommerce.orderservice.domain.model.OrderStatus;
import com.ecommerce.orderservice.domain.port.OrderRepository;
import com.ecommerce.orderservice.domain.service.OrderValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for updating order status.
 * Validates state transitions and delegates to domain model.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateOrderStatusUseCase {
    
    private final OrderRepository orderRepository;
    private final OrderValidationService validationService;
    
    @Transactional
    public Order execute(UUID orderId, OrderStatus newStatus) {
        log.info("Updating status of order {} to {}", orderId, newStatus);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Validate business rule
        if (!validationService.canUpdateStatus(order, newStatus)) {
            throw new InvalidOrderStateException(orderId, order.getStatus(), newStatus);
        }
        
        // Domain logic handles status update
        order.updateStatus(newStatus);
        
        // Persist changes
        Order updatedOrder = orderRepository.save(order);
        
        log.info("Order {} status updated successfully", orderId);
        return updatedOrder;
    }
}

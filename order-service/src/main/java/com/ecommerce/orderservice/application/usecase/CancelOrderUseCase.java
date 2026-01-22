package com.ecommerce.orderservice.application.usecase;

import com.ecommerce.orderservice.domain.exception.CannotCancelOrderException;
import com.ecommerce.orderservice.domain.exception.OrderNotFoundException;
import com.ecommerce.orderservice.domain.model.Order;
import com.ecommerce.orderservice.domain.port.OrderRepository;
import com.ecommerce.orderservice.domain.service.OrderValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for cancelling an order.
 * Validates business rules and delegates to domain model.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CancelOrderUseCase {
    
    private final OrderRepository orderRepository;
    private final OrderValidationService validationService;
    
    @Transactional
    public Order execute(UUID orderId) {
        log.info("Attempting to cancel order with ID: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Validate business rule
        if (!validationService.canCancelOrder(order)) {
            throw new CannotCancelOrderException(orderId, order.getStatus());
        }
        
        // Domain logic handles cancellation
        order.cancel();
        
        // Persist changes
        Order cancelledOrder = orderRepository.save(order);
        
        log.info("Order {} cancelled successfully", orderId);
        return cancelledOrder;
    }
}

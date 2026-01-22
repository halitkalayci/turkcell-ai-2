package com.ecommerce.orderservice.application.usecase;

import com.ecommerce.orderservice.domain.exception.OrderNotFoundException;
import com.ecommerce.orderservice.domain.model.Order;
import com.ecommerce.orderservice.domain.port.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for retrieving an order by ID.
 * Read-only operation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetOrderUseCase {
    
    private final OrderRepository orderRepository;
    
    @Transactional(readOnly = true)
    public Order execute(UUID orderId) {
        log.info("Fetching order with ID: {}", orderId);
        
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}

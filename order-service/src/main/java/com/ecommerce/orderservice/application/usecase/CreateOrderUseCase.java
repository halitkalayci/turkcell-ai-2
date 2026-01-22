package com.ecommerce.orderservice.application.usecase;

import com.ecommerce.orderservice.application.dto.OrderItemDto;
import com.ecommerce.orderservice.application.service.OutboxService;
import com.ecommerce.orderservice.domain.event.OrderCreatedEvent;
import com.ecommerce.orderservice.domain.model.Address;
import com.ecommerce.orderservice.domain.model.Order;
import com.ecommerce.orderservice.domain.model.OrderItem;
import com.ecommerce.orderservice.domain.port.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case for creating a new order.
 * Orchestrates order creation without business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateOrderUseCase {
    
    private final OrderRepository orderRepository;
    private final OutboxService outboxService;
    
    @Transactional
    public Order execute(UUID customerId, Address deliveryAddress, List<OrderItemDto> itemDtos) {
        log.info("Creating order for customer: {}", customerId);
        
        // Convert DTOs to domain models
        List<OrderItem> items = itemDtos.stream()
                .map(dto -> new OrderItem(
                    dto.productId(),
                    dto.productName(),
                    dto.quantity(),
                    dto.unitPrice()
                ))
                .collect(Collectors.toList());
        
        // Create domain order (business logic in domain)
        Order order = new Order(customerId, deliveryAddress, items);
        
        // Persist
        Order savedOrder = orderRepository.save(order);
        
        // Publish OrderCreated event via outbox
        publishOrderCreatedEvent(savedOrder);
        
        log.info("Order created successfully with ID: {}", savedOrder.getId());
        return savedOrder;
    }
    
    private void publishOrderCreatedEvent(Order order) {
        UUID correlationId = UUID.randomUUID(); // In real scenario, get from request context
        
        List<OrderCreatedEvent.OrderItem> eventItems = order.getItems().stream()
            .map(item -> new OrderCreatedEvent.OrderItem(
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice().toString()
            ))
            .collect(Collectors.toList());
        
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            order.getCustomerId(),
            eventItems,
            order.getTotalAmount(),
            correlationId
        );
        
        outboxService.saveToOutbox(event);
        
        log.info("OrderCreated event saved to outbox: orderId={}, correlationId={}", 
            order.getId(), correlationId);
    }
}

package com.ecommerce.orderservice.application.config;

import com.ecommerce.orderservice.application.service.OutboxService;
import com.ecommerce.orderservice.application.usecase.*;
import com.ecommerce.orderservice.domain.port.OrderRepository;
import com.ecommerce.orderservice.domain.service.OrderValidationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for application use-cases.
 * Defines beans for dependency injection.
 */
@Configuration
public class UseCaseConfig {
    
    @Bean
    public CreateOrderUseCase createOrderUseCase(
            OrderRepository orderRepository,
            OutboxService outboxService) {
        return new CreateOrderUseCase(orderRepository, outboxService);
    }
    
    @Bean
    public GetOrderUseCase getOrderUseCase(OrderRepository orderRepository) {
        return new GetOrderUseCase(orderRepository);
    }
    
    @Bean
    public CancelOrderUseCase cancelOrderUseCase(
            OrderRepository orderRepository,
            OrderValidationService validationService) {
        return new CancelOrderUseCase(orderRepository, validationService);
    }
    
    @Bean
    public UpdateOrderStatusUseCase updateOrderStatusUseCase(
            OrderRepository orderRepository,
            OrderValidationService validationService) {
        return new UpdateOrderStatusUseCase(orderRepository, validationService);
    }
}

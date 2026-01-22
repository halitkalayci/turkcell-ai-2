package com.ecommerce.orderservice.infrastructure.persistence.adapter;

import com.ecommerce.orderservice.domain.model.Order;
import com.ecommerce.orderservice.domain.port.OrderRepository;
import com.ecommerce.orderservice.infrastructure.persistence.entity.OrderEntity;
import com.ecommerce.orderservice.infrastructure.persistence.mapper.OrderMapper;
import com.ecommerce.orderservice.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementing the domain OrderRepository port using JPA.
 * Translates between domain and persistence models.
 */
@Component
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {
    
    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;
    
    @Override
    public Order save(Order order) {
        OrderEntity entity = mapper.toEntity(order);
        OrderEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }
    
    @Override
    public Optional<Order> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }
    
    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }
}

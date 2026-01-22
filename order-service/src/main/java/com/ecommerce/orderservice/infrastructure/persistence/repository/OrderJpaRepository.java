package com.ecommerce.orderservice.infrastructure.persistence.repository;

import com.ecommerce.orderservice.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for OrderEntity.
 * Provides CRUD operations for order persistence.
 */
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
}

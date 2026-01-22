package com.ecommerce.orderservice.infrastructure.persistence.mapper;

import com.ecommerce.orderservice.domain.model.Address;
import com.ecommerce.orderservice.domain.model.Order;
import com.ecommerce.orderservice.domain.model.OrderItem;
import com.ecommerce.orderservice.infrastructure.persistence.entity.OrderEntity;
import com.ecommerce.orderservice.infrastructure.persistence.entity.OrderItemEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper between domain Order and JPA OrderEntity.
 * Handles conversion between domain and persistence models.
 */
@Component
public class OrderMapper {
    
    /**
     * Maps domain Order to JPA OrderEntity.
     */
    public OrderEntity toEntity(Order order) {
        if (order == null) {
            return null;
        }
        
        OrderEntity entity = OrderEntity.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .street(order.getDeliveryAddress().getStreet())
                .city(order.getDeliveryAddress().getCity())
                .state(order.getDeliveryAddress().getState())
                .postalCode(order.getDeliveryAddress().getPostalCode())
                .country(order.getDeliveryAddress().getCountry())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .cancelledAt(order.getCancelledAt())
                .build();
        
        // Map items
        List<OrderItemEntity> itemEntities = order.getItems().stream()
                .map(item -> toItemEntity(item, entity))
                .collect(Collectors.toList());
        
        entity.setItems(itemEntities);
        
        return entity;
    }
    
    /**
     * Maps JPA OrderEntity to domain Order.
     */
    public Order toDomain(OrderEntity entity) {
        if (entity == null) {
            return null;
        }
        
        Address address = new Address(
            entity.getStreet(),
            entity.getCity(),
            entity.getState(),
            entity.getPostalCode(),
            entity.getCountry()
        );
        
        List<OrderItem> items = entity.getItems().stream()
                .map(this::toItemDomain)
                .collect(Collectors.toList());
        
        return Order.reconstruct(
            entity.getId(),
            entity.getCustomerId(),
            address,
            items,
            entity.getStatus(),
            entity.getTotalAmount(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getCancelledAt()
        );
    }
    
    /**
     * Maps domain OrderItem to JPA OrderItemEntity.
     */
    private OrderItemEntity toItemEntity(OrderItem item, OrderEntity orderEntity) {
        return OrderItemEntity.builder()
                .order(orderEntity)
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }
    
    /**
     * Maps JPA OrderItemEntity to domain OrderItem.
     */
    private OrderItem toItemDomain(OrderItemEntity entity) {
        return new OrderItem(
            entity.getProductId(),
            entity.getProductName(),
            entity.getQuantity(),
            entity.getUnitPrice(),
            entity.getTotalPrice()
        );
    }
}

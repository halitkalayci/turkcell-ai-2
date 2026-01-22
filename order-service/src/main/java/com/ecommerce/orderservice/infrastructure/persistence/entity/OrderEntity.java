package com.ecommerce.orderservice.infrastructure.persistence.entity;

import com.ecommerce.orderservice.domain.model.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for order persistence.
 * Maps to 'orders' table in database.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    
    @Column(name = "street", nullable = false, length = 200)
    private String street;
    
    @Column(name = "city", nullable = false, length = 100)
    private String city;
    
    @Column(name = "state", length = 100)
    private String state;
    
    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;
    
    @Column(name = "country", nullable = false, length = 100)
    private String country;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItemEntity> items = new ArrayList<>();
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;
    
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "cancelled_at")
    private Instant cancelledAt;
    
    /**
     * Helper method to add an order item and maintain bidirectional relationship
     */
    public void addItem(OrderItemEntity item) {
        items.add(item);
        item.setOrder(this);
    }
}

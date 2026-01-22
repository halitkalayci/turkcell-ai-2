package com.ecommerce.orderservice.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Domain model representing an order aggregate root.
 * Contains all business logic for order management.
 * No framework dependencies - pure domain logic.
 */
public class Order {
    
    private UUID id;
    private final UUID customerId;
    private final Address deliveryAddress;
    private final List<OrderItem> items;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant cancelledAt;
    
    /**
     * Creates a new order (for new order creation).
     * 
     * @param customerId the customer identifier
     * @param deliveryAddress the delivery address
     * @param items the order items (must not be empty)
     * @throws IllegalArgumentException if validation fails
     */
    public Order(UUID customerId, Address deliveryAddress, List<OrderItem> items) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        if (deliveryAddress == null) {
            throw new IllegalArgumentException("Delivery address cannot be null");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.deliveryAddress = deliveryAddress;
        this.items = new ArrayList<>(items);
        this.status = OrderStatus.PREPARING;
        this.totalAmount = calculateTotal();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Reconstruction constructor for persistence.
     * Used when loading an existing order from database.
     */
    public Order(UUID id, UUID customerId, Address deliveryAddress, List<OrderItem> items,
                 OrderStatus status, BigDecimal totalAmount, Instant createdAt, 
                 Instant updatedAt, Instant cancelledAt) {
        if (id == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        if (deliveryAddress == null) {
            throw new IllegalArgumentException("Delivery address cannot be null");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        if (status == null) {
            throw new IllegalArgumentException("Order status cannot be null");
        }
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total amount cannot be negative");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Created timestamp cannot be null");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("Updated timestamp cannot be null");
        }
        
        this.id = id;
        this.customerId = customerId;
        this.deliveryAddress = deliveryAddress;
        this.items = new ArrayList<>(items);
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.cancelledAt = cancelledAt;
    }
    
    /**
     * Factory method for reconstructing orders from persistence.
     */
    public static Order reconstruct(UUID id, UUID customerId, Address deliveryAddress, 
                                   List<OrderItem> items, OrderStatus status, 
                                   BigDecimal totalAmount, Instant createdAt, 
                                   Instant updatedAt, Instant cancelledAt) {
        return new Order(id, customerId, deliveryAddress, items, status, totalAmount, 
                        createdAt, updatedAt, cancelledAt);
    }
    
    /**
     * Cancels the order if it's in a cancellable state.
     * Business rule: can only cancel PREPARING or PENDING orders.
     * 
     * @throws IllegalStateException if order cannot be cancelled
     */
    public void cancel() {
        if (!status.canBeCancelled()) {
            throw new IllegalStateException(
                String.format("Cannot cancel order in %s status. Only PREPARING and PENDING orders can be cancelled.", 
                             status)
            );
        }
        
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Updates the order status with validation.
     * Business rule: cannot change status of cancelled or delivered orders.
     * 
     * @param newStatus the new status
     * @throws IllegalStateException if status transition is invalid
     */
    public void updateStatus(OrderStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        
        if (this.status.isFinalState()) {
            throw new IllegalStateException(
                String.format("Cannot change status of order in final state: %s", this.status)
            );
        }
        
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Calculates total amount from all order items.
     * 
     * @return the total amount
     */
    private BigDecimal calculateTotal() {
        return items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Recalculates and updates the total amount.
     * Used when items are modified (though items are immutable in current design).
     */
    public void recalculateTotal() {
        this.totalAmount = calculateTotal();
        this.updatedAt = Instant.now();
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public UUID getCustomerId() {
        return customerId;
    }
    
    public Address getDeliveryAddress() {
        return deliveryAddress;
    }
    
    /**
     * Returns an unmodifiable view of the order items.
     */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public Instant getCancelledAt() {
        return cancelledAt;
    }
    
    /**
     * Checks if the order has been cancelled.
     */
    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }
    
    /**
     * Checks if the order is in a final state.
     */
    public boolean isInFinalState() {
        return status.isFinalState();
    }
}

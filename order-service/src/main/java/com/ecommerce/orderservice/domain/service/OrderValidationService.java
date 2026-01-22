package com.ecommerce.orderservice.domain.service;

import com.ecommerce.orderservice.domain.model.Order;
import com.ecommerce.orderservice.domain.model.OrderStatus;
import org.springframework.stereotype.Component;

/**
 * Domain service for order validation and business rule enforcement.
 * Contains complex business rules that don't belong to a single entity.
 */
@Component
public class OrderValidationService {
    
    /**
     * Validates if an order can be cancelled based on its current status.
     * Business rule: Only PREPARING and PENDING orders can be cancelled.
     * 
     * @param order the order to check
     * @return true if order can be cancelled
     */
    public boolean canCancelOrder(Order order) {
        if (order == null) {
            return false;
        }
        return order.getStatus().canBeCancelled();
    }
    
    /**
     * Validates if an order status can be updated.
     * Business rule: Cannot update status of orders in final states (DELIVERED, CANCELLED).
     * 
     * @param order the order to check
     * @param newStatus the proposed new status
     * @return true if status can be updated
     */
    public boolean canUpdateStatus(Order order, OrderStatus newStatus) {
        if (order == null || newStatus == null) {
            return false;
        }
        
        // Cannot change status of orders in final states
        if (order.getStatus().isFinalState()) {
            return false;
        }
        
        // Cannot transition to the same status
        if (order.getStatus() == newStatus) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates the order status transition.
     * Ensures business rules for valid state transitions are followed.
     * 
     * @param currentStatus the current status
     * @param newStatus the new status
     * @return true if transition is valid
     */
    public boolean isValidStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }
        
        // Already in final state - no transitions allowed
        if (currentStatus.isFinalState()) {
            return false;
        }
        
        // Can always cancel from PREPARING or PENDING
        if (newStatus == OrderStatus.CANCELLED && currentStatus.canBeCancelled()) {
            return true;
        }
        
        // Valid forward transitions
        return switch (currentStatus) {
            case PREPARING -> newStatus == OrderStatus.PENDING || newStatus == OrderStatus.CANCELLED;
            case PENDING -> newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED -> newStatus == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false; // Final states
        };
    }
}

package com.ecommerce.orderservice.domain.service;

import com.ecommerce.orderservice.domain.model.Order;
import com.ecommerce.orderservice.domain.model.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Domain service for order calculation logic.
 * Handles price calculations and validations.
 */
@Component
public class OrderCalculationService {
    
    /**
     * Calculates the total amount for a list of order items.
     * 
     * @param items the order items
     * @return the total amount
     */
    public BigDecimal calculateOrderTotal(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Validates that the calculated total matches the order's total.
     * Used for integrity checks.
     * 
     * @param order the order to validate
     * @return true if totals match
     */
    public boolean validateOrderTotal(Order order) {
        BigDecimal calculatedTotal = calculateOrderTotal(order.getItems());
        return order.getTotalAmount().compareTo(calculatedTotal) == 0;
    }
    
    /**
     * Calculates the total price for a single item.
     * 
     * @param unitPrice the unit price
     * @param quantity the quantity
     * @return the total price
     */
    public BigDecimal calculateItemTotal(BigDecimal unitPrice, int quantity) {
        if (unitPrice == null || quantity <= 0) {
            return BigDecimal.ZERO;
        }
        
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}

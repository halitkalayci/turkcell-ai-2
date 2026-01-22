package com.ecommerce.orderservice.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a single item in an order.
 * Immutable - once created, values cannot be changed.
 */
public class OrderItem {
    
    private final UUID productId;
    private final String productName;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal totalPrice;
    
    /**
     * Creates a new order item with validation and automatic total calculation.
     * 
     * @param productId the product identifier (must not be null)
     * @param productName the product name (must not be null/empty)
     * @param quantity the quantity ordered (must be positive)
     * @param unitPrice the price per unit (must be positive)
     * @throws IllegalArgumentException if validation fails
     */
    public OrderItem(UUID productId, String productName, int quantity, BigDecimal unitPrice) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive, got: " + quantity);
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit price must be positive, got: " + unitPrice);
        }
        
        this.productId = productId;
        this.productName = productName.trim();
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    /**
     * Reconstruction constructor for persistence (with pre-calculated total).
     * Used when loading from database.
     */
    public OrderItem(UUID productId, String productName, int quantity, BigDecimal unitPrice, BigDecimal totalPrice) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive, got: " + quantity);
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit price must be positive, got: " + unitPrice);
        }
        if (totalPrice == null || totalPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total price cannot be negative, got: " + totalPrice);
        }
        
        this.productId = productId;
        this.productName = productName.trim();
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
    }
    
    public UUID getProductId() {
        return productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public BigDecimal getTotalPrice() {
        return totalPrice;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return quantity == orderItem.quantity &&
               Objects.equals(productId, orderItem.productId) &&
               Objects.equals(productName, orderItem.productName) &&
               Objects.equals(unitPrice, orderItem.unitPrice) &&
               Objects.equals(totalPrice, orderItem.totalPrice);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(productId, productName, quantity, unitPrice, totalPrice);
    }
    
    @Override
    public String toString() {
        return "OrderItem{" +
                "productId=" + productId +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", totalPrice=" + totalPrice +
                '}';
    }
}

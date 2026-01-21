package com.ecommerce.orderservice.entity;

/**
 * Order status enum matching OpenAPI contract.
 * - PREPARING: Order is being prepared for shipment
 * - SHIPPED: Order has been shipped to the customer
 * - DELIVERED: Order has been delivered to the customer
 * - CANCELLED: Order has been cancelled
 */
public enum OrderStatus {
    PREPARING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}

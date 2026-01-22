package com.ecommerce.orderservice.domain.port;

import com.ecommerce.orderservice.domain.model.Order;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain port (interface) for order persistence operations.
 * Defines the contract that infrastructure must implement.
 * Part of hexagonal architecture - allows domain to be independent of infrastructure.
 */
public interface OrderRepository {
    
    /**
     * Saves a new order or updates an existing one.
     * 
     * @param order the order to save
     * @return the saved order with updated timestamps
     */
    Order save(Order order);
    
    /**
     * Finds an order by its unique identifier.
     * 
     * @param id the order ID
     * @return Optional containing the order if found, empty otherwise
     */
    Optional<Order> findById(UUID id);
    
    /**
     * Checks if an order exists by ID.
     * 
     * @param id the order ID
     * @return true if order exists
     */
    boolean existsById(UUID id);
}

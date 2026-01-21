package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.request.AvailabilityCheckRequest;
import com.ecommerce.inventoryservice.dto.request.StockReservationRequest;
import com.ecommerce.inventoryservice.dto.response.AvailabilityCheckResponse;
import com.ecommerce.inventoryservice.dto.response.InventoryItemResponse;
import com.ecommerce.inventoryservice.dto.response.StockReservationResponse;

import java.util.UUID;

/**
 * Service interface for inventory management operations
 * Defines business operations for stock tracking and reservations
 */
public interface InventoryService {

    /**
     * Retrieves stock information for a specific product
     *
     * @param productId UUID of the product
     * @return inventory item response with stock details
     * @throws com.ecommerce.inventoryservice.exception.ProductNotFoundException if product not found
     */
    InventoryItemResponse getInventoryItem(UUID productId);

    /**
     * Checks stock availability for multiple products without reserving
     * Non-destructive operation for validation purposes
     *
     * @param request availability check request with items and quantities
     * @return availability check response indicating stock sufficiency
     */
    AvailabilityCheckResponse checkStockAvailability(AvailabilityCheckRequest request);

    /**
     * Creates a stock reservation for an order
     * Reserves requested quantities and calculates expiration time
     *
     * @param request stock reservation request with order details
     * @return stock reservation response with reservation details
     * @throws com.ecommerce.inventoryservice.exception.ProductNotFoundException if any product not found
     * @throws com.ecommerce.inventoryservice.exception.InsufficientStockException if stock insufficient
     * @throws com.ecommerce.inventoryservice.exception.ReservationConflictException if concurrent modification detected
     */
    StockReservationResponse createStockReservation(StockReservationRequest request);

    /**
     * Retrieves details of an existing reservation
     *
     * @param reservationId UUID of the reservation
     * @return stock reservation response with reservation details
     * @throws com.ecommerce.inventoryservice.exception.ReservationNotFoundException if not found or expired
     */
    StockReservationResponse getReservation(UUID reservationId);

    /**
     * Releases a stock reservation and returns quantities to available stock
     * Idempotent operation - releasing already released/expired reservation succeeds
     *
     * @param reservationId UUID of the reservation to release
     * @throws com.ecommerce.inventoryservice.exception.ReservationNotFoundException if reservation not found
     */
    void releaseReservation(UUID reservationId);
}

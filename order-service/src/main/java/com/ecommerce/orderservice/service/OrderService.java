package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.request.OrderRequest;
import com.ecommerce.orderservice.dto.request.UpdateStatusRequest;
import com.ecommerce.orderservice.dto.response.CancelOrderResponse;
import com.ecommerce.orderservice.dto.response.OrderResponse;

import java.util.UUID;

public interface OrderService {

    /**
     * Creates a new order
     *
     * @param orderRequest the order request containing customer info, address, and items
     * @return the created order response
     */
    OrderResponse createOrder(OrderRequest orderRequest);

    /**
     * Retrieves an order by its ID
     *
     * @param id the order ID
     * @return the order response
     */
    OrderResponse getOrderById(UUID id);

    /**
     * Cancels an order if it's not yet shipped or delivered
     *
     * @param id the order ID
     * @return the cancel order response
     */
    CancelOrderResponse cancelOrder(UUID id);

    /**
     * Updates the status of an order
     *
     * @param id the order ID
     * @param request the update status request
     * @return the updated order response
     */
    OrderResponse updateOrderStatus(UUID id, UpdateStatusRequest request);
}

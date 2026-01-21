package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.request.OrderRequest;
import com.ecommerce.orderservice.dto.request.UpdateStatusRequest;
import com.ecommerce.orderservice.dto.response.CancelOrderResponse;
import com.ecommerce.orderservice.dto.response.OrderResponse;
import com.ecommerce.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Order management operations")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(
            summary = "Create a new order",
            description = "Creates a new order with customer information, delivery address, and list of products"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Order created successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data (validation error)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Business rule violation (e.g., insufficient stock)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest orderRequest) {
        log.info("Received request to create order for customer: {}", orderRequest.getCustomerId());
        OrderResponse response = orderService.createOrder(orderRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get order details by ID",
            description = "Retrieves detailed information about a specific order"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "Unique identifier of the order", required = true)
            @PathVariable UUID id) {
        log.info("Received request to get order: {}", id);
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @Operation(
            summary = "Cancel an order",
            description = "Cancels an order if it has not been shipped yet. Orders with status SHIPPED or DELIVERED cannot be cancelled."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order cancelled successfully",
                    content = @Content(schema = @Schema(implementation = CancelOrderResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Order cannot be cancelled (already shipped or delivered)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<CancelOrderResponse> cancelOrder(
            @Parameter(description = "Unique identifier of the order to cancel", required = true)
            @PathVariable UUID id) {
        log.info("Received request to cancel order: {}", id);
        CancelOrderResponse response = orderService.cancelOrder(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Update order status",
            description = "Updates the status of an order (e.g., from PREPARING to SHIPPED, or SHIPPED to DELIVERED)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order status updated successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid status value",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @Parameter(description = "Unique identifier of the order", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        log.info("Received request to update order {} status to {}", id, request.getStatus());
        OrderResponse response = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(response);
    }
}

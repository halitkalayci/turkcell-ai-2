package com.ecommerce.orderservice.web.controller;

import com.ecommerce.orderservice.application.dto.OrderItemDto;
import com.ecommerce.orderservice.application.usecase.*;
import com.ecommerce.orderservice.domain.model.Address;
import com.ecommerce.orderservice.domain.model.Order;
import com.ecommerce.orderservice.web.dto.request.AddressDto;
import com.ecommerce.orderservice.web.dto.request.OrderRequestDto;
import com.ecommerce.orderservice.web.dto.request.UpdateStatusRequestDto;
import com.ecommerce.orderservice.web.dto.response.CancelOrderResponseDto;
import com.ecommerce.orderservice.web.dto.response.OrderItemResponseDto;
import com.ecommerce.orderservice.web.dto.response.OrderResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for order operations.
 * Maps OpenAPI contract endpoints to application use-cases.
 * Secured with operation-based claims via @PreAuthorize.
 * 
 * Authorization Model:
 * - order.create: Create new orders (Customer, Admin, OrderManager)
 * - order.read.own: Read own orders (Customer)
 * - order.read.all: Read all orders (Admin, OrderManager)
 * - order.delete: Delete orders (Admin, OrderManager)
 * - order.status.update: Update order status (Admin, OrderManager)
 * - order.cancel: Cancel orders (Customer for own, Admin/OrderManager for all)
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    
    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    
    /**
     * Create a new order.
     * Required claim: order.create
     * 
     * @param request Order creation request
     * @param jwt JWT token containing user identity and claims
     * @return Created order response (201 Created)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('order.create')")
    public ResponseEntity<OrderResponseDto> createOrder(
            @Valid @RequestBody OrderRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        
        // Extract user context from JWT
        String userId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        
        log.info("Creating order for customer: {} | User: {} ({})", 
            request.getCustomerId(), username, userId);
        
        Address address = new Address(
            request.getAddress().getStreet(),
            request.getAddress().getCity(),
            request.getAddress().getState(),
            request.getAddress().getPostalCode(),
            request.getAddress().getCountry()
        );
        
        List<OrderItemDto> items = request.getItems().stream()
                .map(item -> new OrderItemDto(
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnitPrice()
                ))
                .collect(Collectors.toList());
        
        Order order = createOrderUseCase.execute(request.getCustomerId(), address, items);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapToOrderResponse(order));
    }
    
    /**
     * Get order by ID.
     * Required claims: order.read.own OR order.read.all
     * 
     * Ownership check:
     * - If user has 'order.read.all', can read any order
     * - If user has only 'order.read.own', can only read their own orders
     * - Service layer enforces ownership validation
     * 
     * @param id Order ID
     * @param jwt JWT token containing user identity and claims
     * @return Order details (200 OK) or 403 Forbidden if not owner
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order.read.own') or hasAuthority('order.read.all')")
    public ResponseEntity<OrderResponseDto> getOrderById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        List<String> orderClaims = jwt.getClaim("order_claims");
        
        boolean canReadAll = orderClaims != null && orderClaims.contains("order.read.all");
        
        log.info("Fetching order {} | User: {} ({}) | CanReadAll: {}", 
            id, username, userId, canReadAll);
        
        // Use case will validate ownership if canReadAll is false
        Order order = getOrderUseCase.execute(id);
        return ResponseEntity.ok(mapToOrderResponse(order));
    }
    
    /**
     * Cancel order (soft delete).
     * Required claim: order.delete
     * 
     * @param id Order ID to cancel
     * @param jwt JWT token containing user identity
     * @return Cancellation confirmation (200 OK)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('order.delete')")
    public ResponseEntity<CancelOrderResponseDto> cancelOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = jwt.getClaimAsString("preferred_username");
        log.info("Cancelling order {} | User: {}", id, username);
        
        Order order = cancelOrderUseCase.execute(id);
        
        CancelOrderResponseDto response = CancelOrderResponseDto.builder()
                .id(order.getId())
                .status(order.getStatus())
                .message("Order has been successfully cancelled")
                .cancelledAt(order.getCancelledAt())
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update order status.
     * Required claim: order.status.update
     * 
     * @param id Order ID
     * @param request Status update request
     * @param jwt JWT token containing user identity
     * @return Updated order (200 OK)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('order.status.update')")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = jwt.getClaimAsString("preferred_username");
        log.info("Updating status of order {} to {} | User: {}", 
            id, request.getStatus(), username);
        
        Order order = updateOrderStatusUseCase.execute(id, request.getStatus());
        return ResponseEntity.ok(mapToOrderResponse(order));
    }
    
    // ========== PRIVATE MAPPING METHODS ==========
    
    private OrderResponseDto mapToOrderResponse(Order order) {
        List<OrderItemResponseDto> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponseDto.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .collect(Collectors.toList());
        
        AddressDto addressDto = AddressDto.builder()
                .street(order.getDeliveryAddress().getStreet())
                .city(order.getDeliveryAddress().getCity())
                .state(order.getDeliveryAddress().getState())
                .postalCode(order.getDeliveryAddress().getPostalCode())
                .country(order.getDeliveryAddress().getCountry())
                .build();
        
        return OrderResponseDto.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .address(addressDto)
                .items(itemResponses)
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}

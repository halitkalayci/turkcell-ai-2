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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for order operations.
 * Maps OpenAPI contract endpoints to application use-cases.
 * No business logic - only request/response mapping and coordination.
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
    
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto request) {
        log.info("Creating order for customer: {}", request.getCustomerId());
        
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
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable UUID id) {
        log.info("Fetching order with ID: {}", id);
        
        Order order = getOrderUseCase.execute(id);
        return ResponseEntity.ok(mapToOrderResponse(order));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<CancelOrderResponseDto> cancelOrder(@PathVariable UUID id) {
        log.info("Cancelling order with ID: {}", id);
        
        Order order = cancelOrderUseCase.execute(id);
        
        CancelOrderResponseDto response = CancelOrderResponseDto.builder()
                .id(order.getId())
                .status(order.getStatus())
                .message("Order has been successfully cancelled")
                .cancelledAt(order.getCancelledAt())
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequestDto request) {
        log.info("Updating status of order {} to {}", id, request.getStatus());
        
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

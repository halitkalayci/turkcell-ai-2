package com.ecommerce.inventoryservice.web.controller;

import com.ecommerce.inventoryservice.application.dto.ProductQuantity;
import com.ecommerce.inventoryservice.application.usecase.*;
import com.ecommerce.inventoryservice.domain.model.InventoryItem;
import com.ecommerce.inventoryservice.domain.model.Reservation;
import com.ecommerce.inventoryservice.domain.model.ReservationItem;
import com.ecommerce.inventoryservice.web.dto.request.AvailabilityCheckRequest;
import com.ecommerce.inventoryservice.web.dto.request.StockReservationRequest;
import com.ecommerce.inventoryservice.web.dto.response.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for inventory operations
 * Maps OpenAPI contract endpoints to application use-cases
 * No business logic - only request/response mapping and coordination
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final GetInventoryItemUseCase getInventoryItemUseCase;
    private final CheckAvailabilityUseCase checkAvailabilityUseCase;
    private final CreateReservationUseCase createReservationUseCase;
    private final GetReservationUseCase getReservationUseCase;
    private final ConfirmReservationUseCase confirmReservationUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;

    @GetMapping("/items/{productId}")
    public ResponseEntity<InventoryItemResponse> getInventoryItem(@PathVariable UUID productId) {
        InventoryItem item = getInventoryItemUseCase.execute(productId);
        return ResponseEntity.ok(mapToInventoryItemResponse(item));
    }

    @PostMapping("/check-availability")
    public ResponseEntity<AvailabilityCheckResponse> checkStockAvailability(
            @Valid @RequestBody AvailabilityCheckRequest request) {

        List<ProductQuantity> products = request.getItems().stream()
                .map(item -> new ProductQuantity(item.getProductId(), item.getQuantity()))
                .collect(Collectors.toList());

        var result = checkAvailabilityUseCase.execute(products);

        AvailabilityCheckResponse response = AvailabilityCheckResponse.builder()
                .available(result.allAvailable())
                .items(result.items().stream()
                        .map(item -> AvailabilityCheckResultItem.builder()
                                .productId(item.productId())
                                .requestedQuantity(item.requestedQuantity())
                                .availableQuantity(item.availableQuantity())
                                .sufficient(item.available())
                                .build())
                        .toList())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reservations")
    public ResponseEntity<StockReservationResponse> createStockReservation(
            @Valid @RequestBody StockReservationRequest request) {

        List<ReservationItem> items = request.getItems().stream()
                .map(item -> new ReservationItem(item.getProductId(), item.getQuantity()))
                .collect(Collectors.toList());

        Reservation reservation = createReservationUseCase.execute(
                request.getOrderId(),
                items,
                request.getReservationTtlMinutes()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapToReservationResponse(reservation));
    }

    @GetMapping("/reservations/{reservationId}")
    public ResponseEntity<StockReservationResponse> getReservation(@PathVariable UUID reservationId) {
        Reservation reservation = getReservationUseCase.execute(reservationId);
        return ResponseEntity.ok(mapToReservationResponse(reservation));
    }

    @PutMapping("/reservations/{reservationId}/confirm")
    public ResponseEntity<Void> confirmReservation(@PathVariable UUID reservationId) {
        confirmReservationUseCase.execute(reservationId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/reservations/{reservationId}")
    public ResponseEntity<Void> releaseReservation(@PathVariable UUID reservationId) {
        cancelReservationUseCase.execute(reservationId);
        return ResponseEntity.noContent().build();
    }

    // ========== PRIVATE MAPPING METHODS ==========

    private InventoryItemResponse mapToInventoryItemResponse(InventoryItem item) {
        return InventoryItemResponse.builder()
                .productId(item.getProductId())
                .availableQuantity(item.getAvailableQuantity())
                .reservedQuantity(item.getReservedQuantity())
                .totalQuantity(item.getTotalQuantity())
                .lastUpdatedAt(item.getLastUpdatedAt())
                .build();
    }

    private StockReservationResponse mapToReservationResponse(Reservation reservation) {
        return StockReservationResponse.builder()
                .reservationId(reservation.getId())
                .orderId(reservation.getOrderId())
                .status(reservation.getStatus().name())
                .items(reservation.getItems().stream()
                        .map(item -> ReservedItem.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .createdAt(reservation.getCreatedAt())
                .expiresAt(reservation.getExpiresAt())
                .build();
    }
}

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
 * REST controller for inventory operations.
 * Maps OpenAPI contract endpoints to application use-cases.
 * Secured with operation-based claims via @PreAuthorize.
 * 
 * Authorization Model:
 * - inventory.read: Read inventory items (Admin, InventoryManager)
 * - inventory.create: Create inventory items (Admin, InventoryManager)
 * - inventory.write: Update inventory items (Admin, InventoryManager)
 * - inventory.delete: Delete inventory items (Admin, InventoryManager)
 * - reservation.create: Create reservations (Admin, OrderManager, InventoryManager)
 * - reservation.release: Release reservations (Admin, OrderManager, InventoryManager)
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final GetInventoryItemUseCase getInventoryItemUseCase;
    private final CheckAvailabilityUseCase checkAvailabilityUseCase;
    private final CreateReservationUseCase createReservationUseCase;
    private final GetReservationUseCase getReservationUseCase;
    private final ConfirmReservationUseCase confirmReservationUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;

    /**
     * Get inventory item by product ID.
     * Required claim: inventory.read
     */
    @GetMapping("/items/{productId}")
    @PreAuthorize("hasAuthority('inventory.read')")
    public ResponseEntity<InventoryItemResponse> getInventoryItem(
            @PathVariable UUID productId,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
        log.debug("Getting inventory item {} | User: {}", productId, username);
        
        InventoryItem item = getInventoryItemUseCase.execute(productId);
        return ResponseEntity.ok(mapToInventoryItemResponse(item));
    }

    /**
     * Check stock availability for multiple items.
     * Required claim: inventory.read
     */
    @PostMapping("/check-availability")
    @PreAuthorize("hasAuthority('inventory.read')")
    public ResponseEntity<AvailabilityCheckResponse> checkStockAvailability(
            @Valid @RequestBody AvailabilityCheckRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
        log.debug("Checking availability for {} items | User: {}", request.getItems().size(), username);

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

    /**
     * Create stock reservation.
     * Required claim: reservation.create
     */
    @PostMapping("/reservations")
    @PreAuthorize("hasAuthority('reservation.create')")
    public ResponseEntity<StockReservationResponse> createStockReservation(
            @Valid @RequestBody StockReservationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
        log.info("Creating reservation for order {} | User: {}", request.getOrderId(), username);

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

    /**
     * Get reservation by ID.
     * Required claim: reservation.view.all
     */
    @GetMapping("/reservations/{reservationId}")
    @PreAuthorize("hasAuthority('reservation.view.all')")
    public ResponseEntity<StockReservationResponse> getReservation(
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
        log.debug("Getting reservation {} | User: {}", reservationId, username);
        
        Reservation reservation = getReservationUseCase.execute(reservationId);
        return ResponseEntity.ok(mapToReservationResponse(reservation));
    }

    /**
     * Confirm reservation (commit stock).
     * Required claim: reservation.confirm
     */
    @PutMapping("/reservations/{reservationId}/confirm")
    @PreAuthorize("hasAuthority('reservation.confirm')")
    public ResponseEntity<Void> confirmReservation(
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
        log.info("Confirming reservation {} | User: {}", reservationId, username);
        
        confirmReservationUseCase.execute(reservationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Release reservation (rollback stock).
     * Required claim: reservation.release
     */
    @DeleteMapping("/reservations/{reservationId}")
    @PreAuthorize("hasAuthority('reservation.release')")
    public ResponseEntity<Void> releaseReservation(
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
        log.info("Releasing reservation {} | User: {}", reservationId, username);
        
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

package com.ecommerce.inventoryservice.controller;

import com.ecommerce.inventoryservice.dto.request.AvailabilityCheckRequest;
import com.ecommerce.inventoryservice.dto.request.StockReservationRequest;
import com.ecommerce.inventoryservice.dto.response.AvailabilityCheckResponse;
import com.ecommerce.inventoryservice.dto.response.InventoryItemResponse;
import com.ecommerce.inventoryservice.dto.response.StockReservationResponse;
import com.ecommerce.inventoryservice.service.InventoryService;
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

/**
 * REST controller for inventory management operations
 * Implements all inventory service endpoints per OpenAPI contract
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inventory Service", description = "Stock management and reservation operations")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/inventory-items/{productId}")
    @Operation(
            summary = "Get stock information for a product",
            description = "Retrieves current available quantity and stock status for a specific product"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock information retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found in inventory",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<InventoryItemResponse> getInventoryItem(
            @Parameter(description = "Unique identifier of the product", required = true)
            @PathVariable UUID productId) {
        log.debug("GET /api/v1/inventory-items/{}", productId);

        InventoryItemResponse response = inventoryService.getInventoryItem(productId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/inventory-items/availability-check")
    @Operation(
            summary = "Check stock availability for multiple products",
            description = "Validates whether requested quantities are available for multiple products. " +
                    "This is a non-destructive operation that does not reserve stock."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Availability check completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AvailabilityCheckResponse> checkStockAvailability(
            @Valid @RequestBody AvailabilityCheckRequest request) {
        log.debug("POST /api/v1/inventory-items/availability-check with {} items", request.getItems().size());

        AvailabilityCheckResponse response = inventoryService.checkStockAvailability(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/inventory-reservations")
    @Operation(
            summary = "Create a stock reservation for an order",
            description = "Reserves stock for the specified products and quantities. " +
                    "Returns a reservation ID that must be used to release the reservation later. " +
                    "Reservations automatically expire after TTL (default: 15 minutes)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reservation created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "One or more products not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Reservation conflict due to concurrent modification",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Insufficient stock to fulfill reservation",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<StockReservationResponse> createStockReservation(
            @Valid @RequestBody StockReservationRequest request) {
        log.debug("POST /api/v1/inventory-reservations for order {}", request.getOrderId());

        StockReservationResponse response = inventoryService.createStockReservation(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/inventory-reservations/{reservationId}")
    @Operation(
            summary = "Get reservation details",
            description = "Retrieves detailed information about an existing stock reservation"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Reservation not found or expired",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<StockReservationResponse> getReservation(
            @Parameter(description = "Unique identifier of the reservation", required = true)
            @PathVariable UUID reservationId) {
        log.debug("GET /api/v1/inventory-reservations/{}", reservationId);

        StockReservationResponse response = inventoryService.getReservation(reservationId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/inventory-reservations/{reservationId}")
    @Operation(
            summary = "Release a stock reservation",
            description = "Releases a previously created stock reservation, returning the reserved quantities back to available stock. " +
                    "This operation is idempotent - releasing an already released or expired reservation returns 204."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Reservation released successfully"),
            @ApiResponse(responseCode = "404", description = "Reservation not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> releaseReservation(
            @Parameter(description = "Unique identifier of the reservation", required = true)
            @PathVariable UUID reservationId) {
        log.debug("DELETE /api/v1/inventory-reservations/{}", reservationId);

        inventoryService.releaseReservation(reservationId);

        return ResponseEntity.noContent().build();
    }
}

package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.request.AvailabilityCheckRequest;
import com.ecommerce.inventoryservice.dto.request.StockReservationRequest;
import com.ecommerce.inventoryservice.dto.response.*;
import com.ecommerce.inventoryservice.entity.InventoryItem;
import com.ecommerce.inventoryservice.entity.Reservation;
import com.ecommerce.inventoryservice.entity.ReservationStatus;
import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.exception.ProductNotFoundException;
import com.ecommerce.inventoryservice.exception.ReservationConflictException;
import com.ecommerce.inventoryservice.exception.ReservationNotFoundException;
import com.ecommerce.inventoryservice.repository.InventoryItemRepository;
import com.ecommerce.inventoryservice.repository.ReservationRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of inventory service business logic
 * Handles stock tracking, availability checks, and reservation lifecycle
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final ReservationRepository reservationRepository;

    @Override
    @Transactional(readOnly = true)
    public InventoryItemResponse getInventoryItem(UUID productId) {
        log.debug("Fetching inventory item for product: {}", productId);

        InventoryItem inventoryItem = inventoryItemRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        log.debug("Found inventory item: productId={}, available={}, reserved={}, total={}",
                inventoryItem.getProductId(),
                inventoryItem.getAvailableQuantity(),
                inventoryItem.getReservedQuantity(),
                inventoryItem.getTotalQuantity());

        return mapToInventoryItemResponse(inventoryItem);
    }

    @Override
    @Transactional(readOnly = true)
    public AvailabilityCheckResponse checkStockAvailability(AvailabilityCheckRequest request) {
        log.debug("Checking stock availability for {} items", request.getItems().size());

        List<AvailabilityCheckResultItem> resultItems = new ArrayList<>();
        boolean allAvailable = true;

        for (var item : request.getItems()) {
            InventoryItem inventoryItem = inventoryItemRepository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException(item.getProductId()));

            boolean sufficient = inventoryItem.isAvailable(item.getQuantity());
            if (!sufficient) {
                allAvailable = false;
            }

            AvailabilityCheckResultItem resultItem = AvailabilityCheckResultItem.builder()
                    .productId(item.getProductId())
                    .requestedQuantity(item.getQuantity())
                    .availableQuantity(inventoryItem.getAvailableQuantity())
                    .sufficient(sufficient)
                    .build();

            resultItems.add(resultItem);

            log.debug("Availability check: productId={}, requested={}, available={}, sufficient={}",
                    item.getProductId(), item.getQuantity(),
                    inventoryItem.getAvailableQuantity(), sufficient);
        }

        log.debug("Overall availability check result: allAvailable={}", allAvailable);

        return AvailabilityCheckResponse.builder()
                .available(allAvailable)
                .items(resultItems)
                .build();
    }

    @Override
    @Transactional
    public StockReservationResponse createStockReservation(StockReservationRequest request) {
        log.debug("Creating stock reservation for order: {}", request.getOrderId());

        try {
            // Step 1: Validate all products exist and check stock sufficiency
            List<InsufficientStockItem> insufficientItems = new ArrayList<>();

            for (var item : request.getItems()) {
                InventoryItem inventoryItem = inventoryItemRepository.findByProductId(item.getProductId())
                        .orElseThrow(() -> new ProductNotFoundException(item.getProductId()));

                if (!inventoryItem.isAvailable(item.getQuantity())) {
                    insufficientItems.add(InsufficientStockItem.builder()
                            .productId(item.getProductId())
                            .requestedQuantity(item.getQuantity())
                            .availableQuantity(inventoryItem.getAvailableQuantity())
                            .build());
                }
            }

            // Step 2: If any insufficient, throw exception with details
            if (!insufficientItems.isEmpty()) {
                log.warn("Insufficient stock for {} items", insufficientItems.size());
                throw new InsufficientStockException(insufficientItems);
            }

            // Step 3: Create reservation entity
            Reservation reservation = Reservation.builder()
                    .orderId(request.getOrderId())
                    .status(ReservationStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMinutes(
                            request.getReservationTtlMinutes() != null ? request.getReservationTtlMinutes() : 15))
                    .items(new ArrayList<>())
                    .build();

            // Step 4: Reserve stock for each item
            for (var item : request.getItems()) {
                InventoryItem inventoryItem = inventoryItemRepository.findByProductId(item.getProductId())
                        .orElseThrow(() -> new ProductNotFoundException(item.getProductId()));

                // Update stock levels
                inventoryItem.setAvailableQuantity(inventoryItem.getAvailableQuantity() - item.getQuantity());
                inventoryItem.setReservedQuantity(inventoryItem.getReservedQuantity() + item.getQuantity());
                inventoryItem.recalculateTotalQuantity();

                inventoryItemRepository.save(inventoryItem);

                // Create reservation item
                com.ecommerce.inventoryservice.entity.ReservationItem reservationItem =
                        com.ecommerce.inventoryservice.entity.ReservationItem.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .build();

                reservation.addItem(reservationItem);

                log.debug("Reserved {} units of product {} (new available: {}, reserved: {})",
                        item.getQuantity(), item.getProductId(),
                        inventoryItem.getAvailableQuantity(),
                        inventoryItem.getReservedQuantity());
            }

            // Step 5: Save reservation
            Reservation savedReservation = reservationRepository.save(reservation);

            log.info("Created reservation {} for order {} with {} items, expires at {}",
                    savedReservation.getReservationId(),
                    savedReservation.getOrderId(),
                    savedReservation.getItems().size(),
                    savedReservation.getExpiresAt());

            return mapToReservationResponse(savedReservation);

        } catch (OptimisticLockException e) {
            log.error("Optimistic lock conflict during reservation creation", e);
            throw new ReservationConflictException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StockReservationResponse getReservation(UUID reservationId) {
        log.debug("Fetching reservation: {}", reservationId);

        Reservation reservation = reservationRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        // Check if expired
        if (reservation.isExpired()) {
            log.warn("Reservation {} has expired", reservationId);
            throw new ReservationNotFoundException(reservationId);
        }

        log.debug("Found reservation: reservationId={}, orderId={}, status={}",
                reservation.getReservationId(),
                reservation.getOrderId(),
                reservation.getStatus());

        return mapToReservationResponse(reservation);
    }

    @Override
    @Transactional
    public void releaseReservation(UUID reservationId) {
        log.debug("Releasing reservation: {}", reservationId);

        Reservation reservation = reservationRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        // Idempotent: if already released or expired, return successfully
        if (reservation.getStatus() == ReservationStatus.RELEASED ||
                reservation.getStatus() == ReservationStatus.EXPIRED) {
            log.debug("Reservation {} already in terminal state: {}", reservationId, reservation.getStatus());
            return;
        }

        try {
            // Return reserved quantities to available stock
            for (var item : reservation.getItems()) {
                InventoryItem inventoryItem = inventoryItemRepository.findByProductId(item.getProductId())
                        .orElseThrow(() -> new ProductNotFoundException(item.getProductId()));

                inventoryItem.setAvailableQuantity(inventoryItem.getAvailableQuantity() + item.getQuantity());
                inventoryItem.setReservedQuantity(inventoryItem.getReservedQuantity() - item.getQuantity());
                inventoryItem.recalculateTotalQuantity();

                inventoryItemRepository.save(inventoryItem);

                log.debug("Released {} units of product {} (new available: {}, reserved: {})",
                        item.getQuantity(), item.getProductId(),
                        inventoryItem.getAvailableQuantity(),
                        inventoryItem.getReservedQuantity());
            }

            // Update reservation status
            reservation.setStatus(ReservationStatus.RELEASED);
            reservationRepository.save(reservation);

            log.info("Released reservation {} for order {}", reservationId, reservation.getOrderId());

        } catch (OptimisticLockException e) {
            log.error("Optimistic lock conflict during reservation release", e);
            throw new ReservationConflictException();
        }
    }

    /**
     * Maps InventoryItem entity to response DTO
     */
    private InventoryItemResponse mapToInventoryItemResponse(InventoryItem inventoryItem) {
        return InventoryItemResponse.builder()
                .productId(inventoryItem.getProductId())
                .availableQuantity(inventoryItem.getAvailableQuantity())
                .reservedQuantity(inventoryItem.getReservedQuantity())
                .totalQuantity(inventoryItem.getTotalQuantity())
                .lastUpdatedAt(inventoryItem.getLastUpdatedAt())
                .build();
    }

    /**
     * Maps Reservation entity to response DTO
     */
    private StockReservationResponse mapToReservationResponse(Reservation reservation) {
        List<ReservedItem> reservedItems = reservation.getItems().stream()
                .map(item -> ReservedItem.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        return StockReservationResponse.builder()
                .reservationId(reservation.getReservationId())
                .orderId(reservation.getOrderId())
                .status(reservation.getStatus().name())
                .items(reservedItems)
                .createdAt(reservation.getCreatedAt())
                .expiresAt(reservation.getExpiresAt())
                .build();
    }
}

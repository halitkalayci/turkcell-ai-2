package com.ecommerce.inventoryservice.config;

import com.ecommerce.inventoryservice.application.usecase.*;
import com.ecommerce.inventoryservice.domain.port.InventoryRepository;
import com.ecommerce.inventoryservice.domain.port.ReservationRepository;
import com.ecommerce.inventoryservice.domain.service.ReservationService;
import com.ecommerce.inventoryservice.domain.service.StockService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for application layer use-cases.
 * Manual bean configuration enables pure hexagonal architecture:
 * - Use-cases are NOT annotated with @Service
 * - Can be instantiated without Spring for testing
 * - Clear dependency injection via constructors
 */
@Configuration
public class UseCaseConfig {

    /**
     * Bean for retrieving inventory item information.
     */
    @Bean
    public GetInventoryItemUseCase getInventoryItemUseCase(
            InventoryRepository inventoryRepository) {
        return new GetInventoryItemUseCase(inventoryRepository);
    }

    /**
     * Bean for checking stock availability across multiple products.
     */
    @Bean
    public CheckAvailabilityUseCase checkAvailabilityUseCase(
            InventoryRepository inventoryRepository,
            StockService stockService) {
        return new CheckAvailabilityUseCase(inventoryRepository, stockService);
    }

    /**
     * Bean for creating new stock reservations.
     * Transactional operation with complex orchestration.
     */
    @Bean
    public CreateReservationUseCase createReservationUseCase(
            InventoryRepository inventoryRepository,
            ReservationRepository reservationRepository,
            StockService stockService,
            ReservationService reservationService) {
        return new CreateReservationUseCase(
            inventoryRepository,
            reservationRepository,
            stockService,
            reservationService
        );
    }

    /**
     * Bean for retrieving reservation information.
     */
    @Bean
    public GetReservationUseCase getReservationUseCase(
            ReservationRepository reservationRepository) {
        return new GetReservationUseCase(reservationRepository);
    }

    /**
     * Bean for confirming reservations (order payment successful).
     */
    @Bean
    public ConfirmReservationUseCase confirmReservationUseCase(
            ReservationRepository reservationRepository,
            ReservationService reservationService) {
        return new ConfirmReservationUseCase(
            reservationRepository,
            reservationService
        );
    }

    /**
     * Bean for cancelling reservations and releasing stock.
     * Transactional operation coordinating inventory and reservation updates.
     */
    @Bean
    public CancelReservationUseCase cancelReservationUseCase(
            ReservationRepository reservationRepository,
            InventoryRepository inventoryRepository,
            ReservationService reservationService) {
        return new CancelReservationUseCase(
            reservationRepository,
            inventoryRepository,
            reservationService
        );
    }
}

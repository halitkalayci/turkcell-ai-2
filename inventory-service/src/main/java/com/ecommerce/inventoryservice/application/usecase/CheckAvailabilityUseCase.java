package com.ecommerce.inventoryservice.application.usecase;

import com.ecommerce.inventoryservice.application.dto.AvailabilityResult;
import com.ecommerce.inventoryservice.application.dto.ItemAvailability;
import com.ecommerce.inventoryservice.application.dto.ProductQuantity;
import com.ecommerce.inventoryservice.domain.exception.ProductNotFoundException;
import com.ecommerce.inventoryservice.domain.model.InventoryItem;
import com.ecommerce.inventoryservice.domain.port.InventoryRepository;
import com.ecommerce.inventoryservice.domain.service.StockService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use-case: Check if multiple products have sufficient stock availability.
 * Read-only operation - no transaction required.
 * 
 * Orchestrates:
 * 1. Load inventory items from repository
 * 2. Validate all products exist
 * 3. Check availability via domain service
 * 4. Build result with per-item details
 */
public class CheckAvailabilityUseCase {

    private final InventoryRepository inventoryRepository;
    private final StockService stockService;

    public CheckAvailabilityUseCase(InventoryRepository inventoryRepository,
                                    StockService stockService) {
        this.inventoryRepository = inventoryRepository;
        this.stockService = stockService;
    }

    /**
     * Checks stock availability for multiple products.
     *
     * @param items list of products with requested quantities
     * @return availability result with overall status and per-item details
     * @throws ProductNotFoundException if any product doesn't exist
     */
    public AvailabilityResult execute(List<ProductQuantity> items) {
        // 1. Extract product IDs
        List<UUID> productIds = items.stream()
            .map(ProductQuantity::productId)
            .toList();

        // 2. Load inventory items
        List<InventoryItem> inventoryItems = inventoryRepository.findByProductIds(productIds);

        // 3. Validate all products exist
        validateAllProductsExist(productIds, inventoryItems);

        // 4. Build inventory map for quick lookup
        Map<UUID, InventoryItem> inventoryMap = inventoryItems.stream()
            .collect(Collectors.toMap(InventoryItem::getProductId, item -> item));

        // 5. Build requested quantities map
        Map<UUID, Integer> requestedQuantities = items.stream()
            .collect(Collectors.toMap(ProductQuantity::productId, ProductQuantity::quantity));

        // 6. Check overall availability using domain service
        boolean allAvailable = stockService.checkMultipleAvailability(
            requestedQuantities, 
            inventoryItems
        );

        // 7. Build per-item availability results
        List<ItemAvailability> itemResults = buildItemResults(items, inventoryMap);

        return new AvailabilityResult(allAvailable, itemResults);
    }

    private void validateAllProductsExist(List<UUID> requestedIds, List<InventoryItem> foundItems) {
        if (foundItems.size() != requestedIds.size()) {
            List<UUID> foundIds = foundItems.stream()
                .map(InventoryItem::getProductId)
                .toList();
            
            List<UUID> missingIds = new ArrayList<>(requestedIds);
            missingIds.removeAll(foundIds);
            
            if (!missingIds.isEmpty()) {
                throw new ProductNotFoundException(missingIds.get(0));
            }
        }
    }

    private List<ItemAvailability> buildItemResults(List<ProductQuantity> items,
                                                     Map<UUID, InventoryItem> inventoryMap) {
        return items.stream()
            .map(item -> {
                InventoryItem inventoryItem = inventoryMap.get(item.productId());
                boolean available = inventoryItem.isAvailable(item.quantity());
                
                return new ItemAvailability(
                    item.productId(),
                    available,
                    item.quantity(),
                    inventoryItem.getAvailableQuantity()
                );
            })
            .toList();
    }
}

package com.ecommerce.inventoryservice.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Event published when items are successfully reserved.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ItemsReservedEvent extends BaseEvent {
    
    private ItemsReservedPayload payload;
    
    public ItemsReservedEvent(UUID reservationId, UUID orderId, List<ReservedItem> items, UUID correlationId) {
        super("ItemsReserved", reservationId, correlationId);
        this.payload = new ItemsReservedPayload(reservationId, orderId, items);
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemsReservedPayload {
        private UUID reservationId;
        private UUID orderId;
        private List<ReservedItem> items;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservedItem {
        private UUID productId;
        private Integer quantity;
        private Integer reservedQuantity;
    }
}

package com.ecommerce.orderservice.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Event received when items are successfully reserved in inventory.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ItemsReservedEvent extends BaseEvent {
    
    private ItemsReservedPayload payload;
    
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

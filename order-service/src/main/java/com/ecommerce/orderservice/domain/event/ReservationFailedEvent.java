package com.ecommerce.orderservice.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Event received when reservation fails in Inventory Service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReservationFailedEvent extends BaseEvent {
    
    private ReservationFailedPayload payload;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationFailedPayload {
        private UUID orderId;
        private String reason;
        private List<UnavailableItem> unavailableItems;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnavailableItem {
        private UUID productId;
        private Integer requestedQuantity;
        private Integer availableQuantity;
    }
}

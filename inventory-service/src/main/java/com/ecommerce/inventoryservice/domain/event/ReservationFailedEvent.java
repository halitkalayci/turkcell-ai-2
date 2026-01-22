package com.ecommerce.inventoryservice.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Event published when reservation fails due to insufficient stock or other issues.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReservationFailedEvent extends BaseEvent {
    
    private ReservationFailedPayload payload;
    
    public ReservationFailedEvent(UUID orderId, String reason, List<UnavailableItem> unavailableItems, UUID correlationId) {
        super("ReservationFailed", orderId, correlationId);
        this.payload = new ReservationFailedPayload(orderId, reason, unavailableItems);
    }
    
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

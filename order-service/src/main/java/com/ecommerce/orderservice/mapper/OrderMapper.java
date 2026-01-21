package com.ecommerce.orderservice.mapper;

import com.ecommerce.orderservice.api.model.*;
import com.ecommerce.orderservice.domain.Order;
import com.ecommerce.orderservice.domain.OrderItem;
import com.ecommerce.orderservice.domain.OrderStatus;
import org.mapstruct.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface OrderMapper {

    // Map OrderRequest to Order entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toEntity(OrderRequest orderRequest);

    // Map Order entity to OrderResponse
    OrderResponse toResponse(Order order);

    // Map OrderItem entity to OrderItemResponse
    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "productName", source = "productName")
    OrderItemResponse toItemResponse(OrderItem orderItem);

    // Map OrderItemRequest to OrderItem entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productName", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "order", ignore = true)
    OrderItem toItemEntity(OrderItemRequest orderItemRequest);

    // Map list of OrderItemRequest to list of OrderItem entities
    List<OrderItem> toItemEntities(List<OrderItemRequest> orderItemRequests);

    // Map Address DTO to Address entity
    com.ecommerce.orderservice.domain.Address toAddressEntity(Address addressDto);

    // Map Address entity to Address DTO
    Address toAddressDto(com.ecommerce.orderservice.domain.Address addressEntity);

    // Map OrderStatus enum to API model enum
    default com.ecommerce.orderservice.api.model.OrderStatus toApiStatus(OrderStatus status) {
        if (status == null) {
            return null;
        }
        return com.ecommerce.orderservice.api.model.OrderStatus.fromValue(status.name());
    }

    // Map API model OrderStatus to domain OrderStatus
    default OrderStatus toDomainStatus(com.ecommerce.orderservice.api.model.OrderStatus apiStatus) {
        if (apiStatus == null) {
            return null;
        }
        return OrderStatus.valueOf(apiStatus.getValue());
    }

    // Map Order to CancelOrderResponse
    @Mapping(target = "message", constant = "Order has been successfully cancelled")
    @Mapping(target = "cancelledAt", source = "updatedAt")
    CancelOrderResponse toCancelResponse(Order order);

    // Helper method to map UUID to String
    default String uuidToString(UUID uuid) {
        return uuid != null ? uuid.toString() : null;
    }

    // Helper method to map String to UUID
    default UUID stringToUuid(String string) {
        return string != null ? UUID.fromString(string) : null;
    }

    // Helper method to map OffsetDateTime to String
    default String offsetDateTimeToString(OffsetDateTime dateTime) {
        return dateTime != null ? dateTime.toString() : null;
    }
}

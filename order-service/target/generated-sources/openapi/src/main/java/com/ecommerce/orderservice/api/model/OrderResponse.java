package com.ecommerce.orderservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.ecommerce.orderservice.api.model.Address;
import com.ecommerce.orderservice.api.model.OrderItemResponse;
import com.ecommerce.orderservice.api.model.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * OrderResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-21T11:26:59.713127900+03:00[Europe/Istanbul]")
public class OrderResponse {

  private UUID id;

  private UUID customerId;

  private Address address;

  @Valid
  private List<@Valid OrderItemResponse> items;

  private OrderStatus status;

  private Double totalAmount;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public OrderResponse id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Unique identifier of the order
   * @return id
  */
  @Valid 
  @Schema(name = "id", example = "650e8400-e29b-41d4-a716-446655440000", description = "Unique identifier of the order", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public OrderResponse customerId(UUID customerId) {
    this.customerId = customerId;
    return this;
  }

  /**
   * Unique identifier of the customer
   * @return customerId
  */
  @Valid 
  @Schema(name = "customerId", example = "123e4567-e89b-12d3-a456-426614174000", description = "Unique identifier of the customer", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("customerId")
  public UUID getCustomerId() {
    return customerId;
  }

  public void setCustomerId(UUID customerId) {
    this.customerId = customerId;
  }

  public OrderResponse address(Address address) {
    this.address = address;
    return this;
  }

  /**
   * Get address
   * @return address
  */
  @Valid 
  @Schema(name = "address", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("address")
  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public OrderResponse items(List<@Valid OrderItemResponse> items) {
    this.items = items;
    return this;
  }

  public OrderResponse addItemsItem(OrderItemResponse itemsItem) {
    if (this.items == null) {
      this.items = new ArrayList<>();
    }
    this.items.add(itemsItem);
    return this;
  }

  /**
   * List of items in the order
   * @return items
  */
  @Valid 
  @Schema(name = "items", description = "List of items in the order", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("items")
  public List<@Valid OrderItemResponse> getItems() {
    return items;
  }

  public void setItems(List<@Valid OrderItemResponse> items) {
    this.items = items;
  }

  public OrderResponse status(OrderStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
  */
  @Valid 
  @Schema(name = "status", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("status")
  public OrderStatus getStatus() {
    return status;
  }

  public void setStatus(OrderStatus status) {
    this.status = status;
  }

  public OrderResponse totalAmount(Double totalAmount) {
    this.totalAmount = totalAmount;
    return this;
  }

  /**
   * Total amount of the order in the system currency
   * @return totalAmount
  */
  
  @Schema(name = "totalAmount", example = "299.99", description = "Total amount of the order in the system currency", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("totalAmount")
  public Double getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(Double totalAmount) {
    this.totalAmount = totalAmount;
  }

  public OrderResponse createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Timestamp when the order was created
   * @return createdAt
  */
  @Valid 
  @Schema(name = "createdAt", example = "2026-01-21T10:30Z", description = "Timestamp when the order was created", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OrderResponse updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * Timestamp when the order was last updated
   * @return updatedAt
  */
  @Valid 
  @Schema(name = "updatedAt", example = "2026-01-21T14:45Z", description = "Timestamp when the order was last updated", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("updatedAt")
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OrderResponse orderResponse = (OrderResponse) o;
    return Objects.equals(this.id, orderResponse.id) &&
        Objects.equals(this.customerId, orderResponse.customerId) &&
        Objects.equals(this.address, orderResponse.address) &&
        Objects.equals(this.items, orderResponse.items) &&
        Objects.equals(this.status, orderResponse.status) &&
        Objects.equals(this.totalAmount, orderResponse.totalAmount) &&
        Objects.equals(this.createdAt, orderResponse.createdAt) &&
        Objects.equals(this.updatedAt, orderResponse.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, customerId, address, items, status, totalAmount, createdAt, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OrderResponse {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    customerId: ").append(toIndentedString(customerId)).append("\n");
    sb.append("    address: ").append(toIndentedString(address)).append("\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    totalAmount: ").append(toIndentedString(totalAmount)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}


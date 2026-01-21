package com.ecommerce.orderservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.ecommerce.orderservice.api.model.Address;
import com.ecommerce.orderservice.api.model.OrderItemRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * OrderRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-21T11:26:59.713127900+03:00[Europe/Istanbul]")
public class OrderRequest {

  private UUID customerId;

  private Address address;

  @Valid
  private List<@Valid OrderItemRequest> items = new ArrayList<>();

  /**
   * Default constructor
   * @deprecated Use {@link OrderRequest#OrderRequest(UUID, Address, List<@Valid OrderItemRequest>)}
   */
  @Deprecated
  public OrderRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public OrderRequest(UUID customerId, Address address, List<@Valid OrderItemRequest> items) {
    this.customerId = customerId;
    this.address = address;
    this.items = items;
  }

  public OrderRequest customerId(UUID customerId) {
    this.customerId = customerId;
    return this;
  }

  /**
   * Unique identifier of the customer placing the order
   * @return customerId
  */
  @NotNull @Valid 
  @Schema(name = "customerId", example = "123e4567-e89b-12d3-a456-426614174000", description = "Unique identifier of the customer placing the order", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("customerId")
  public UUID getCustomerId() {
    return customerId;
  }

  public void setCustomerId(UUID customerId) {
    this.customerId = customerId;
  }

  public OrderRequest address(Address address) {
    this.address = address;
    return this;
  }

  /**
   * Get address
   * @return address
  */
  @NotNull @Valid 
  @Schema(name = "address", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("address")
  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public OrderRequest items(List<@Valid OrderItemRequest> items) {
    this.items = items;
    return this;
  }

  public OrderRequest addItemsItem(OrderItemRequest itemsItem) {
    if (this.items == null) {
      this.items = new ArrayList<>();
    }
    this.items.add(itemsItem);
    return this;
  }

  /**
   * List of products to be ordered
   * @return items
  */
  @NotNull @Valid @Size(min = 1) 
  @Schema(name = "items", example = "[{\"productId\":\"550e8400-e29b-41d4-a716-446655440001\",\"quantity\":2},{\"productId\":\"550e8400-e29b-41d4-a716-446655440002\",\"quantity\":1}]", description = "List of products to be ordered", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("items")
  public List<@Valid OrderItemRequest> getItems() {
    return items;
  }

  public void setItems(List<@Valid OrderItemRequest> items) {
    this.items = items;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OrderRequest orderRequest = (OrderRequest) o;
    return Objects.equals(this.customerId, orderRequest.customerId) &&
        Objects.equals(this.address, orderRequest.address) &&
        Objects.equals(this.items, orderRequest.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(customerId, address, items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OrderRequest {\n");
    sb.append("    customerId: ").append(toIndentedString(customerId)).append("\n");
    sb.append("    address: ").append(toIndentedString(address)).append("\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
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


package com.ecommerce.orderservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
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
 * OrderItemRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-21T11:26:59.713127900+03:00[Europe/Istanbul]")
public class OrderItemRequest {

  private UUID productId;

  private Integer quantity;

  /**
   * Default constructor
   * @deprecated Use {@link OrderItemRequest#OrderItemRequest(UUID, Integer)}
   */
  @Deprecated
  public OrderItemRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public OrderItemRequest(UUID productId, Integer quantity) {
    this.productId = productId;
    this.quantity = quantity;
  }

  public OrderItemRequest productId(UUID productId) {
    this.productId = productId;
    return this;
  }

  /**
   * Unique identifier of the product
   * @return productId
  */
  @NotNull @Valid 
  @Schema(name = "productId", example = "550e8400-e29b-41d4-a716-446655440001", description = "Unique identifier of the product", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("productId")
  public UUID getProductId() {
    return productId;
  }

  public void setProductId(UUID productId) {
    this.productId = productId;
  }

  public OrderItemRequest quantity(Integer quantity) {
    this.quantity = quantity;
    return this;
  }

  /**
   * Quantity of the product to order
   * minimum: 1
   * @return quantity
  */
  @NotNull @Min(1) 
  @Schema(name = "quantity", example = "2", description = "Quantity of the product to order", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("quantity")
  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OrderItemRequest orderItemRequest = (OrderItemRequest) o;
    return Objects.equals(this.productId, orderItemRequest.productId) &&
        Objects.equals(this.quantity, orderItemRequest.quantity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(productId, quantity);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OrderItemRequest {\n");
    sb.append("    productId: ").append(toIndentedString(productId)).append("\n");
    sb.append("    quantity: ").append(toIndentedString(quantity)).append("\n");
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


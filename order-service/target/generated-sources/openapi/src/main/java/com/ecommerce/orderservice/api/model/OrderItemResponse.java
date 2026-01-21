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
 * OrderItemResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-21T11:26:59.713127900+03:00[Europe/Istanbul]")
public class OrderItemResponse {

  private UUID id;

  private UUID productId;

  private String productName;

  private Integer quantity;

  private Double unitPrice;

  private Double totalPrice;

  public OrderItemResponse id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Unique identifier of the order item
   * @return id
  */
  @Valid 
  @Schema(name = "id", example = "750e8400-e29b-41d4-a716-446655440000", description = "Unique identifier of the order item", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public OrderItemResponse productId(UUID productId) {
    this.productId = productId;
    return this;
  }

  /**
   * Unique identifier of the product
   * @return productId
  */
  @Valid 
  @Schema(name = "productId", example = "550e8400-e29b-41d4-a716-446655440001", description = "Unique identifier of the product", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("productId")
  public UUID getProductId() {
    return productId;
  }

  public void setProductId(UUID productId) {
    this.productId = productId;
  }

  public OrderItemResponse productName(String productName) {
    this.productName = productName;
    return this;
  }

  /**
   * Name of the product
   * @return productName
  */
  
  @Schema(name = "productName", example = "Wireless Mouse", description = "Name of the product", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("productName")
  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public OrderItemResponse quantity(Integer quantity) {
    this.quantity = quantity;
    return this;
  }

  /**
   * Quantity of the product ordered
   * @return quantity
  */
  
  @Schema(name = "quantity", example = "2", description = "Quantity of the product ordered", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("quantity")
  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public OrderItemResponse unitPrice(Double unitPrice) {
    this.unitPrice = unitPrice;
    return this;
  }

  /**
   * Price per unit at the time of order
   * @return unitPrice
  */
  
  @Schema(name = "unitPrice", example = "49.99", description = "Price per unit at the time of order", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("unitPrice")
  public Double getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(Double unitPrice) {
    this.unitPrice = unitPrice;
  }

  public OrderItemResponse totalPrice(Double totalPrice) {
    this.totalPrice = totalPrice;
    return this;
  }

  /**
   * Total price for this item (unitPrice × quantity)
   * @return totalPrice
  */
  
  @Schema(name = "totalPrice", example = "99.98", description = "Total price for this item (unitPrice × quantity)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("totalPrice")
  public Double getTotalPrice() {
    return totalPrice;
  }

  public void setTotalPrice(Double totalPrice) {
    this.totalPrice = totalPrice;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OrderItemResponse orderItemResponse = (OrderItemResponse) o;
    return Objects.equals(this.id, orderItemResponse.id) &&
        Objects.equals(this.productId, orderItemResponse.productId) &&
        Objects.equals(this.productName, orderItemResponse.productName) &&
        Objects.equals(this.quantity, orderItemResponse.quantity) &&
        Objects.equals(this.unitPrice, orderItemResponse.unitPrice) &&
        Objects.equals(this.totalPrice, orderItemResponse.totalPrice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, productId, productName, quantity, unitPrice, totalPrice);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OrderItemResponse {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    productId: ").append(toIndentedString(productId)).append("\n");
    sb.append("    productName: ").append(toIndentedString(productName)).append("\n");
    sb.append("    quantity: ").append(toIndentedString(quantity)).append("\n");
    sb.append("    unitPrice: ").append(toIndentedString(unitPrice)).append("\n");
    sb.append("    totalPrice: ").append(toIndentedString(totalPrice)).append("\n");
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


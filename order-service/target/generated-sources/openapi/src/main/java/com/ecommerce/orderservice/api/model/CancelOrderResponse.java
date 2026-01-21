package com.ecommerce.orderservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.ecommerce.orderservice.api.model.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
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
 * CancelOrderResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-21T11:26:59.713127900+03:00[Europe/Istanbul]")
public class CancelOrderResponse {

  private UUID id;

  private OrderStatus status;

  private String message;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime cancelledAt;

  public CancelOrderResponse id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Unique identifier of the cancelled order
   * @return id
  */
  @Valid 
  @Schema(name = "id", example = "650e8400-e29b-41d4-a716-446655440000", description = "Unique identifier of the cancelled order", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public CancelOrderResponse status(OrderStatus status) {
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

  public CancelOrderResponse message(String message) {
    this.message = message;
    return this;
  }

  /**
   * Confirmation message
   * @return message
  */
  
  @Schema(name = "message", example = "Order has been successfully cancelled", description = "Confirmation message", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public CancelOrderResponse cancelledAt(OffsetDateTime cancelledAt) {
    this.cancelledAt = cancelledAt;
    return this;
  }

  /**
   * Timestamp when the order was cancelled
   * @return cancelledAt
  */
  @Valid 
  @Schema(name = "cancelledAt", example = "2026-01-21T15:30Z", description = "Timestamp when the order was cancelled", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("cancelledAt")
  public OffsetDateTime getCancelledAt() {
    return cancelledAt;
  }

  public void setCancelledAt(OffsetDateTime cancelledAt) {
    this.cancelledAt = cancelledAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CancelOrderResponse cancelOrderResponse = (CancelOrderResponse) o;
    return Objects.equals(this.id, cancelOrderResponse.id) &&
        Objects.equals(this.status, cancelOrderResponse.status) &&
        Objects.equals(this.message, cancelOrderResponse.message) &&
        Objects.equals(this.cancelledAt, cancelOrderResponse.cancelledAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, status, message, cancelledAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CancelOrderResponse {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    cancelledAt: ").append(toIndentedString(cancelledAt)).append("\n");
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


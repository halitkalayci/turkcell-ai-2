package com.ecommerce.orderservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.ecommerce.orderservice.api.model.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * UpdateStatusRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-21T11:26:59.713127900+03:00[Europe/Istanbul]")
public class UpdateStatusRequest {

  private OrderStatus status;

  /**
   * Default constructor
   * @deprecated Use {@link UpdateStatusRequest#UpdateStatusRequest(OrderStatus)}
   */
  @Deprecated
  public UpdateStatusRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UpdateStatusRequest(OrderStatus status) {
    this.status = status;
  }

  public UpdateStatusRequest status(OrderStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
  */
  @NotNull @Valid 
  @Schema(name = "status", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public OrderStatus getStatus() {
    return status;
  }

  public void setStatus(OrderStatus status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateStatusRequest updateStatusRequest = (UpdateStatusRequest) o;
    return Objects.equals(this.status, updateStatusRequest.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateStatusRequest {\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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


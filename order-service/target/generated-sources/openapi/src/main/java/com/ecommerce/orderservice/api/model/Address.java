package com.ecommerce.orderservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Address
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-21T11:26:59.713127900+03:00[Europe/Istanbul]")
public class Address {

  private String street;

  private String city;

  private String state;

  private String postalCode;

  private String country;

  /**
   * Default constructor
   * @deprecated Use {@link Address#Address(String, String, String, String)}
   */
  @Deprecated
  public Address() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Address(String street, String city, String postalCode, String country) {
    this.street = street;
    this.city = city;
    this.postalCode = postalCode;
    this.country = country;
  }

  public Address street(String street) {
    this.street = street;
    return this;
  }

  /**
   * Street address including building number
   * @return street
  */
  @NotNull @Size(min = 1, max = 200) 
  @Schema(name = "street", example = "123 Main Street, Apt 4B", description = "Street address including building number", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("street")
  public String getStreet() {
    return street;
  }

  public void setStreet(String street) {
    this.street = street;
  }

  public Address city(String city) {
    this.city = city;
    return this;
  }

  /**
   * City name
   * @return city
  */
  @NotNull @Size(min = 1, max = 100) 
  @Schema(name = "city", example = "Istanbul", description = "City name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("city")
  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public Address state(String state) {
    this.state = state;
    return this;
  }

  /**
   * State or province (optional)
   * @return state
  */
  @Size(max = 100) 
  @Schema(name = "state", example = "Marmara", description = "State or province (optional)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("state")
  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public Address postalCode(String postalCode) {
    this.postalCode = postalCode;
    return this;
  }

  /**
   * Postal or ZIP code
   * @return postalCode
  */
  @NotNull @Size(min = 1, max = 20) 
  @Schema(name = "postalCode", example = "34000", description = "Postal or ZIP code", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("postalCode")
  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  public Address country(String country) {
    this.country = country;
    return this;
  }

  /**
   * Country name or ISO code
   * @return country
  */
  @NotNull @Size(min = 1, max = 100) 
  @Schema(name = "country", example = "Turkey", description = "Country name or ISO code", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("country")
  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Address address = (Address) o;
    return Objects.equals(this.street, address.street) &&
        Objects.equals(this.city, address.city) &&
        Objects.equals(this.state, address.state) &&
        Objects.equals(this.postalCode, address.postalCode) &&
        Objects.equals(this.country, address.country);
  }

  @Override
  public int hashCode() {
    return Objects.hash(street, city, state, postalCode, country);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Address {\n");
    sb.append("    street: ").append(toIndentedString(street)).append("\n");
    sb.append("    city: ").append(toIndentedString(city)).append("\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
    sb.append("    postalCode: ").append(toIndentedString(postalCode)).append("\n");
    sb.append("    country: ").append(toIndentedString(country)).append("\n");
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


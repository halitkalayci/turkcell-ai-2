package com.ecommerce.orderservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @NotBlank(message = "Street must not be blank")
    @Size(min = 1, max = 200, message = "Street must be between 1 and 200 characters")
    private String street;

    @NotBlank(message = "City must not be blank")
    @Size(min = 1, max = 100, message = "City must be between 1 and 100 characters")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @NotBlank(message = "Postal code must not be blank")
    @Size(min = 1, max = 20, message = "Postal code must be between 1 and 20 characters")
    private String postalCode;

    @NotBlank(message = "Country must not be blank")
    @Size(min = 1, max = 100, message = "Country must be between 1 and 100 characters")
    private String country;
}

package com.ecommerce.orderservice.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for delivery address in requests.
 * Maps to Address schema in OpenAPI contract.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDto {
    
    @NotBlank(message = "Street is required")
    private String street;
    
    @NotBlank(message = "City is required")
    private String city;
    
    private String state;
    
    @NotBlank(message = "Postal code is required")
    private String postalCode;
    
    @NotBlank(message = "Country is required")
    private String country;
}

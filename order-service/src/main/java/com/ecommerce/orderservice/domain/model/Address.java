package com.ecommerce.orderservice.domain.model;

import java.util.Objects;

/**
 * Value object representing a delivery address.
 * Immutable - once created, values cannot be changed.
 */
public class Address {
    
    private final String street;
    private final String city;
    private final String state;
    private final String postalCode;
    private final String country;
    
    /**
     * Creates a new address with validation.
     * 
     * @param street the street address (required)
     * @param city the city (required)
     * @param state the state/province (optional)
     * @param postalCode the postal/zip code (required)
     * @param country the country (required)
     * @throws IllegalArgumentException if required fields are null/empty
     */
    public Address(String street, String city, String state, String postalCode, String country) {
        if (street == null || street.trim().isEmpty()) {
            throw new IllegalArgumentException("Street cannot be null or empty");
        }
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("City cannot be null or empty");
        }
        if (postalCode == null || postalCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Postal code cannot be null or empty");
        }
        if (country == null || country.trim().isEmpty()) {
            throw new IllegalArgumentException("Country cannot be null or empty");
        }
        
        this.street = street.trim();
        this.city = city.trim();
        this.state = state != null ? state.trim() : null;
        this.postalCode = postalCode.trim();
        this.country = country.trim();
    }
    
    public String getStreet() {
        return street;
    }
    
    public String getCity() {
        return city;
    }
    
    public String getState() {
        return state;
    }
    
    public String getPostalCode() {
        return postalCode;
    }
    
    public String getCountry() {
        return country;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(street, address.street) &&
               Objects.equals(city, address.city) &&
               Objects.equals(state, address.state) &&
               Objects.equals(postalCode, address.postalCode) &&
               Objects.equals(country, address.country);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(street, city, state, postalCode, country);
    }
    
    @Override
    public String toString() {
        return "Address{" +
                "street='" + street + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}

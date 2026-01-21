package com.ecommerce.orderservice.exception;

public class BusinessRuleViolationException extends RuntimeException {
    
    public BusinessRuleViolationException(String message) {
        super(message);
    }
}

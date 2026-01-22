package com.ecommerce.orderservice.web.exception;

import com.ecommerce.orderservice.domain.exception.CannotCancelOrderException;
import com.ecommerce.orderservice.domain.exception.InvalidOrderStateException;
import com.ecommerce.orderservice.domain.exception.OrderNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Global exception handler implementing RFC 7807 Problem Details for HTTP APIs.
 * Handles all order service exceptions and converts them to standardized problem details.
 * Maps domain exceptions to appropriate HTTP status codes.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFoundException(OrderNotFoundException ex, WebRequest request) {
        log.error("Order not found: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle("Order Not Found");
        problemDetail.setProperty("instance", getRequestPath(request));
        problemDetail.setProperty("orderId", ex.getOrderId());
        
        return problemDetail;
    }
    
    @ExceptionHandler(CannotCancelOrderException.class)
    public ProblemDetail handleCannotCancelOrderException(CannotCancelOrderException ex, WebRequest request) {
        log.error("Cannot cancel order: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle("Cannot Cancel Order");
        problemDetail.setProperty("instance", getRequestPath(request));
        problemDetail.setProperty("orderId", ex.getOrderId());
        problemDetail.setProperty("currentStatus", ex.getCurrentStatus());
        
        return problemDetail;
    }
    
    @ExceptionHandler(InvalidOrderStateException.class)
    public ProblemDetail handleInvalidOrderStateException(InvalidOrderStateException ex, WebRequest request) {
        log.error("Invalid order state: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle("Invalid Order State");
        problemDetail.setProperty("instance", getRequestPath(request));
        if (ex.getOrderId() != null) {
            problemDetail.setProperty("orderId", ex.getOrderId());
            problemDetail.setProperty("currentStatus", ex.getCurrentStatus());
            problemDetail.setProperty("attemptedStatus", ex.getAttemptedStatus());
        }
        
        return problemDetail;
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        log.error("Validation error: {}", ex.getMessage());
        
        String errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    String fieldName = error instanceof FieldError ? ((FieldError) error).getField() : error.getObjectName();
                    String message = error.getDefaultMessage();
                    return String.format("%s: %s", fieldName, message);
                })
                .collect(Collectors.joining("; "));
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed: " + errors
        );
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle("Bad Request");
        problemDetail.setProperty("instance", getRequestPath(request));
        
        return problemDetail;
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.error("Illegal argument: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle("Bad Request");
        problemDetail.setProperty("instance", getRequestPath(request));
        
        return problemDetail;
    }
    
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        log.error("Illegal state: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle("Unprocessable Entity");
        problemDetail.setProperty("instance", getRequestPath(request));
        
        return problemDetail;
    }
    
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, WebRequest request) {
        log.error("Internal server error", ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("instance", getRequestPath(request));
        
        return problemDetail;
    }
    
    private String getRequestPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}

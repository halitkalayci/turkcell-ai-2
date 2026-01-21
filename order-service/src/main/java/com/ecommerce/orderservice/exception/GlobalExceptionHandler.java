package com.ecommerce.orderservice.exception;

import com.ecommerce.orderservice.api.model.ProblemDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ProblemDetails> handleOrderNotFoundException(
            OrderNotFoundException ex, WebRequest request) {
        
        log.error("Order not found: {}", ex.getMessage());

        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setType("about:blank");
        problemDetails.setTitle("Not Found");
        problemDetails.setStatus(HttpStatus.NOT_FOUND.value());
        problemDetails.setDetail(ex.getMessage());
        problemDetails.setInstance(request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetails);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ProblemDetails> handleBusinessRuleViolationException(
            BusinessRuleViolationException ex, WebRequest request) {
        
        log.error("Business rule violation: {}", ex.getMessage());

        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setType("about:blank");
        problemDetails.setTitle("Business Rule Violation");
        problemDetails.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        problemDetails.setDetail(ex.getMessage());
        problemDetails.setInstance(request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problemDetails);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetails> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        log.error("Validation error: {}", ex.getMessage());

        String errorDetails = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("%s: %s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(", "));

        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setType("about:blank");
        problemDetails.setTitle("Bad Request");
        problemDetails.setStatus(HttpStatus.BAD_REQUEST.value());
        problemDetails.setDetail("Validation failed: " + errorDetails);
        problemDetails.setInstance(request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetails);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetails> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        log.error("Invalid argument: {}", ex.getMessage());

        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setType("about:blank");
        problemDetails.setTitle("Bad Request");
        problemDetails.setStatus(HttpStatus.BAD_REQUEST.value());
        problemDetails.setDetail(ex.getMessage());
        problemDetails.setInstance(request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetails);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetails> handleGlobalException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected error occurred", ex);

        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setType("about:blank");
        problemDetails.setTitle("Internal Server Error");
        problemDetails.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        problemDetails.setDetail("An unexpected error occurred. Please try again later.");
        problemDetails.setInstance(request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetails);
    }
}

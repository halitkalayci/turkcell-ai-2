package com.ecommerce.inventoryservice.web.exception;

import com.ecommerce.inventoryservice.domain.exception.*;
import com.ecommerce.inventoryservice.web.dto.response.InsufficientStockItem;
import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler implementing RFC 7807 Problem Details for HTTP APIs
 * Handles all inventory service exceptions and converts them to standardized problem details
 * Maps domain exceptions to appropriate HTTP status codes
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFoundException(ProductNotFoundException ex, WebRequest request) {
        log.error("Product not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle("Product Not Found");
        problemDetail.setProperty("instance", getRequestPath(request));

        return problemDetail;
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ProblemDetail handleReservationNotFoundException(ReservationNotFoundException ex, WebRequest request) {
        log.error("Reservation not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle("Reservation Not Found");
        problemDetail.setProperty("instance", getRequestPath(request));

        return problemDetail;
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStockException(InsufficientStockException ex, WebRequest request) {
        log.error("Insufficient stock: {}", ex.getMessage());

        // Map domain ProductQuantity to web InsufficientStockItem
        List<InsufficientStockItem> insufficientItems = ex.getInsufficientItems().stream()
                .map(item -> InsufficientStockItem.builder()
                        .productId(item.productId())
                        .requestedQuantity(item.requestedQuantity())
                        .availableQuantity(item.availableQuantity())
                        .build())
                .collect(Collectors.toList());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle("Insufficient Stock");
        problemDetail.setProperty("instance", getRequestPath(request));
        problemDetail.setProperty("insufficientItems", insufficientItems);

        return problemDetail;
    }

    @ExceptionHandler(ReservationConflictException.class)
    public ProblemDetail handleReservationConflictException(ReservationConflictException ex, WebRequest request) {
        log.error("Reservation conflict: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle("Reservation Conflict");
        problemDetail.setProperty("instance", getRequestPath(request));

        return problemDetail;
    }

    @ExceptionHandler(InvalidReservationStateException.class)
    public ProblemDetail handleInvalidReservationStateException(InvalidReservationStateException ex, WebRequest request) {
        log.error("Invalid reservation state: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle("Invalid Reservation State");
        problemDetail.setProperty("instance", getRequestPath(request));

        return problemDetail;
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ProblemDetail handleOptimisticLockException(OptimisticLockException ex, WebRequest request) {
        log.error("Optimistic lock conflict detected", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Stock levels changed during reservation. Please retry the operation."
        );
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle("Reservation Conflict");
        problemDetail.setProperty("instance", getRequestPath(request));

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

    /**
     * Extracts the request path from WebRequest
     */
    private String getRequestPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}

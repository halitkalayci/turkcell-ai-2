package com.ecommerce.inventoryservice.exception;

import com.ecommerce.inventoryservice.dto.response.InsufficientStockItem;
import lombok.Getter;

import java.util.List;

/**
 * Exception thrown when requested quantities cannot be reserved due to insufficient stock
 * Results in HTTP 422 Unprocessable Entity response with detailed insufficient items
 */
@Getter
public class InsufficientStockException extends RuntimeException {

    private final List<InsufficientStockItem> insufficientItems;

    public InsufficientStockException(String message, List<InsufficientStockItem> insufficientItems) {
        super(message);
        this.insufficientItems = insufficientItems;
    }

    public InsufficientStockException(List<InsufficientStockItem> insufficientItems) {
        super("Cannot reserve requested quantities due to insufficient stock");
        this.insufficientItems = insufficientItems;
    }
}

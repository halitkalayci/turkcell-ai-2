package com.ecommerce.inventoryservice.domain.exception;

import com.ecommerce.inventoryservice.application.dto.ProductQuantity;
import lombok.Getter;

import java.util.List;

/**
 * Exception thrown when requested quantities cannot be reserved due to insufficient stock.
 * Contains details about which products have insufficient stock.
 */
@Getter
public class InsufficientStockException extends RuntimeException {

    private final List<ProductQuantity> insufficientItems;

    public InsufficientStockException(String message, List<ProductQuantity> insufficientItems) {
        super(message);
        this.insufficientItems = insufficientItems;
    }

    public InsufficientStockException(List<ProductQuantity> insufficientItems) {
        super("Cannot reserve requested quantities due to insufficient stock");
        this.insufficientItems = insufficientItems;
    }
}

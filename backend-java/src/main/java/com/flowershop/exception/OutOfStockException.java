package com.flowershop.exception;

/**
 * Thrown when a flower has insufficient stock for the requested quantity.
 * Maps to HTTP 400 Bad Request via GlobalExceptionHandler.
 */
public class OutOfStockException extends RuntimeException {
    public OutOfStockException(String message) {
        super(message);
    }
}

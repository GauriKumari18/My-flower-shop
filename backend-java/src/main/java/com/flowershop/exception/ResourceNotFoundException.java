package com.flowershop.exception;

/**
 * Thrown when a requested resource (User, Flower, Order, CartItem) is not
 * found.
 * Maps to HTTP 404 Not Found via GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

package com.flowershop.exception;

/**
 * Thrown when a user tries to checkout with an empty cart.
 * Maps to HTTP 400 Bad Request via GlobalExceptionHandler.
 */
public class EmptyCartException extends RuntimeException {
    public EmptyCartException(String message) {
        super(message);
    }
}

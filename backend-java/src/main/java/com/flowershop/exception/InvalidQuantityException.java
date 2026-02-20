package com.flowershop.exception;

/**
 * Thrown when a quantity value is invalid (e.g., negative or zero where not
 * allowed).
 * Maps to HTTP 400 Bad Request via GlobalExceptionHandler.
 */
public class InvalidQuantityException extends RuntimeException {
    public InvalidQuantityException(String message) {
        super(message);
    }
}

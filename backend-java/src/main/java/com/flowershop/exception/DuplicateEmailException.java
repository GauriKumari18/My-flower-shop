package com.flowershop.exception;

/**
 * Thrown when a user tries to register with an email that already exists.
 * Maps to HTTP 409 Conflict via GlobalExceptionHandler.
 */
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) {
        super(message);
    }
}

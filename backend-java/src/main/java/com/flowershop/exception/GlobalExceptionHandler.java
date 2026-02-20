package com.flowershop.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GLOBAL EXCEPTION HANDLER
 * ------------------------
 * Catches ALL exceptions thrown from any controller/service and returns
 * a consistent JSON error response. Keeps controllers clean — no try-catch
 * needed.
 *
 * Consistent error format:
 * {
 * "timestamp": "2026-02-20T12:21:29",
 * "status": 400,
 * "error": "Bad Request",
 * "message": "Descriptive message here",
 * "path": "/api/flowers"
 * }
 *
 * Spring picks the MOST SPECIFIC handler for each exception type.
 * e.g. ResourceNotFoundException is caught by handleResourceNotFound, NOT
 * handleRuntimeException.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 404 Not Found ────────────────────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request.getRequestURI());
    }

    // ── 409 Conflict (duplicate registration) ────────────────────────────────
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(
            DuplicateEmailException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request.getRequestURI());
    }

    // ── 400 Out of Stock ─────────────────────────────────────────────────────
    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<Map<String, Object>> handleOutOfStock(
            OutOfStockException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Out of Stock", ex.getMessage(), request.getRequestURI());
    }

    // ── 400 Invalid Quantity ─────────────────────────────────────────────────
    @ExceptionHandler(InvalidQuantityException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidQuantity(
            InvalidQuantityException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Quantity", ex.getMessage(), request.getRequestURI());
    }

    // ── 400 Empty Cart ───────────────────────────────────────────────────────
    @ExceptionHandler(EmptyCartException.class)
    public ResponseEntity<Map<String, Object>> handleEmptyCart(
            EmptyCartException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Empty Cart", ex.getMessage(), request.getRequestURI());
    }

    // ── 400 Generic validation errors ────────────────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request.getRequestURI());
    }

    // ── 500 Any other RuntimeException ───────────────────────────────────────
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error", ex.getMessage(), request.getRequestURI());
    }

    // ── 500 Catch-all ────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(
            Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error", ex.getMessage(), request.getRequestURI());
    }

    // ── Private builder ──────────────────────────────────────────────────────
    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String error, String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", path);
        return ResponseEntity.status(status).body(body);
    }
}

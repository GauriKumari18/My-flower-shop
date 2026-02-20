package com.flowershop.controller;

import com.flowershop.model.Order;
import com.flowershop.service.OrderService;
import com.flowershop.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ORDER CONTROLLER
 * ----------------
 * Split into two clear sections:
 *
 * ── CUSTOMER ENDPOINTS (/api/orders) ─────────────────────────────────────
 * POST /api/orders/place → place order from cart
 * GET /api/orders/my-orders → view own order history
 * PATCH /api/orders/{orderId}/cancel → cancel a PENDING order
 *
 * ── ADMIN ENDPOINTS (/api/admin/orders) ──────────────────────────────────
 * GET /api/admin/orders → view ALL orders (optionally filtered)
 * PATCH /api/admin/orders/{id}/status → change any order's status
 *
 * Role enforcement:
 * - /api/admin/** → ADMIN authority only (enforced in SecurityConfig)
 * - /api/orders/** → any authenticated user (enforced in SecurityConfig)
 * - A CUSTOMER cannot call /api/admin/orders → 403 Forbidden
 * - An ADMIN cannot be treated as CUSTOMER (their JWT role is "ADMIN")
 *
 * Error handling → GlobalExceptionHandler (no try-catch here).
 */
@RestController
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    public OrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CUSTOMER ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * POST /api/orders/place
     * Header: Authorization: Bearer <CUSTOMER or ADMIN token>
     *
     * Request body (optional):
     * { "deliveryAddress": "123 Main Street, Mumbai" }
     *
     * What happens:
     * 1. User's cart is loaded
     * 2. Stock validated for every item → OutOfStockException if short
     * 3. Empty cart rejected → EmptyCartException
     * 4. Stock deducted, prices snapshotted
     * 5. Order saved, cart cleared
     *
     * Response: 201 Created + saved order
     */
    @PostMapping("/api/orders/place")
    public ResponseEntity<Order> placeOrder(
            Authentication authentication,
            @RequestBody(required = false) Map<String, String> body) {

        Long userId = resolveUserId(authentication);
        String deliveryAddress = (body != null) ? body.get("deliveryAddress") : null;
        Order order = orderService.placeOrder(userId, deliveryAddress);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * GET /api/orders/my-orders
     * Header: Authorization: Bearer <token>
     *
     * Returns all orders placed by the current user, newest first.
     */
    @GetMapping("/api/orders/my-orders")
    public ResponseEntity<List<Order>> getMyOrders(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(orderService.getMyOrders(userId));
    }

    /**
     * PATCH /api/orders/{orderId}/cancel
     * Header: Authorization: Bearer <token>
     *
     * Customer cancels their OWN PENDING order.
     * Stock is automatically restored on cancellation.
     *
     * Response: 200 + updated order (status = CANCELLED)
     * 400 → not PENDING, or not the user's own order
     */
    @PatchMapping("/api/orders/{orderId}/cancel")
    public ResponseEntity<Order> cancelOrder(
            Authentication authentication,
            @PathVariable Long orderId) {

        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(orderService.cancelOrder(userId, orderId));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ADMIN ENDPOINTS — protected by SecurityConfig: hasAuthority("ADMIN")
    // A CUSTOMER calling these gets 403 Forbidden automatically.
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/admin/orders
     * GET /api/admin/orders?status=PENDING
     * Header: Authorization: Bearer <ADMIN token>
     *
     * Returns ALL orders. Optional ?status= param filters by order status.
     * Valid status values: PENDING | CONFIRMED | CANCELLED | DELIVERED
     */
    @GetMapping("/api/admin/orders")
    public ResponseEntity<List<Order>> getAllOrders(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderService.getAllOrders(status));
    }

    /**
     * PATCH /api/admin/orders/{orderId}/status
     * Header: Authorization: Bearer <ADMIN token>
     *
     * Request body: { "status": "CONFIRMED" }
     *
     * Valid transitions:
     * PENDING → CONFIRMED or CANCELLED
     * CONFIRMED → DELIVERED or CANCELLED (stock restored if cancelled)
     * DELIVERED → terminal, no changes
     * CANCELLED → terminal, no changes
     *
     * Response: 200 + updated order
     */
    @PatchMapping("/api/admin/orders/{orderId}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> body) {

        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("Status field is required in the request body.");
        }
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, newStatus));
    }

    // ─── PRIVATE HELPER ───────────────────────────────────────────────────────

    /**
     * Resolves the userId from the JWT token via email lookup.
     * authentication.getName() → email (set as JWT subject in JwtService).
     */
    private Long resolveUserId(Authentication authentication) {
        return userService.getUserIdByEmail(authentication.getName());
    }
}

package com.flowershop.controller;

import com.flowershop.service.CartService;
import com.flowershop.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * CART CONTROLLER
 * ---------------
 * Shopping cart endpoints. All require a valid JWT (any authenticated user).
 *
 * The logged-in user is identified from the JWT token — no userId in the URL.
 * Authentication.getName() → email → UserService.getUserIdByEmail() → userId.
 *
 * Endpoints:
 * GET /api/cart → view own cart + total
 * POST /api/cart/add → add flower to cart
 * PATCH /api/cart/items/{itemId}/quantity → update quantity (0 = remove)
 * DELETE /api/cart/items/{itemId} → remove one item
 * DELETE /api/cart/clear → empty the entire cart
 *
 * Error handling → GlobalExceptionHandler (no try-catch here).
 * Role enforcement → SecurityConfig (.anyRequest().authenticated()).
 */
@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    public CartController(CartService cartService, UserService userService) {
        this.cartService = cartService;
        this.userService = userService;
    }

    // ─── GET CART ─────────────────────────────────────────────────────────────

    /**
     * GET /api/cart
     * Header: Authorization: Bearer <token>
     *
     * Response:
     * {
     * "cartId": 1,
     * "items": [ { "id": 1, "flower": {...}, "quantity": 2 }, ... ],
     * "totalPrice": 14.97,
     * "itemCount": 2
     * }
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getCart(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(cartService.getCartSummary(userId));
    }

    // ─── ADD ITEM ─────────────────────────────────────────────────────────────

    /**
     * POST /api/cart/add
     * Header: Authorization: Bearer <token>
     *
     * Request body:
     * {
     * "flowerId": 3,
     * "quantity": 2
     * }
     *
     * Responses:
     * 200 OK → updated cart summary
     * 400 → negative quantity, out of stock, flower not found
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addToCart(
            Authentication authentication,
            @RequestBody Map<String, Integer> request) {

        Long userId = resolveUserId(authentication);
        Long flowerId = Long.valueOf(request.get("flowerId"));
        Integer quantity = request.get("quantity");

        cartService.addItem(userId, flowerId, quantity);
        return ResponseEntity.ok(cartService.getCartSummary(userId));
    }

    // ─── UPDATE QUANTITY ──────────────────────────────────────────────────────

    /**
     * PATCH /api/cart/items/{itemId}/quantity
     * Header: Authorization: Bearer <token>
     *
     * Request body: { "quantity": 5 }
     * Set quantity to 0 to remove the item entirely.
     */
    @PatchMapping("/items/{itemId}/quantity")
    public ResponseEntity<Map<String, Object>> updateQuantity(
            Authentication authentication,
            @PathVariable Long itemId,
            @RequestBody Map<String, Integer> request) {

        Long userId = resolveUserId(authentication);
        Integer newQty = request.get("quantity");

        cartService.updateQuantity(userId, itemId, newQty);
        return ResponseEntity.ok(cartService.getCartSummary(userId));
    }

    // ─── REMOVE ONE ITEM ──────────────────────────────────────────────────────

    /**
     * DELETE /api/cart/items/{itemId}
     * Header: Authorization: Bearer <token>
     * Response: 200 + updated cart summary
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Map<String, Object>> removeItem(
            Authentication authentication,
            @PathVariable Long itemId) {

        Long userId = resolveUserId(authentication);
        cartService.removeItem(userId, itemId);
        return ResponseEntity.ok(cartService.getCartSummary(userId));
    }

    // ─── CLEAR CART ───────────────────────────────────────────────────────────

    /**
     * DELETE /api/cart/clear
     * Header: Authorization: Bearer <token>
     * Response: 200 + confirmation message
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearCart(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        cartService.clearCart(userId);
        return ResponseEntity.ok(Map.of("message", "Cart cleared successfully."));
    }

    // ─── PRIVATE HELPER ───────────────────────────────────────────────────────

    /**
     * Extracts the userId of the currently authenticated user from their JWT.
     * authentication.getName() returns the email (set as subject in JwtService).
     */
    private Long resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userService.getUserIdByEmail(email);
    }
}

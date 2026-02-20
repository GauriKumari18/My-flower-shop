package com.flowershop.service;

import com.flowershop.exception.InvalidQuantityException;
import com.flowershop.exception.OutOfStockException;
import com.flowershop.exception.ResourceNotFoundException;
import com.flowershop.model.*;
import com.flowershop.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * CART SERVICE
 * ------------
 * All cart business logic.
 *
 * Exceptions thrown (caught by GlobalExceptionHandler):
 * InvalidQuantityException → 400 (quantity < 1 or negative)
 * OutOfStockException → 400 (requested qty > available stock)
 * ResourceNotFoundException → 404 (flower, user, or cart item not found)
 * IllegalArgumentException → 400 (item doesn't belong to user's cart)
 */
@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final FlowerRepository flowerRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            FlowerRepository flowerRepository,
            UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.flowerRepository = flowerRepository;
        this.userRepository = userRepository;
    }

    // ─── GET OR CREATE CART ───────────────────────────────────────────────────

    /**
     * Returns the user's active cart, creating an empty one if none exists.
     * Safe entry point — always call this before any cart operation.
     */
    public Cart getOrCreateCart(Long userId) {
        User user = findUserById(userId);
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder().user(user).build();
                    return cartRepository.save(newCart);
                });
    }

    // ─── ADD ITEM ─────────────────────────────────────────────────────────────

    /**
     * Adds a flower to the cart, or increments quantity if already present.
     *
     * Validation:
     * ✗ Cannot add negative quantity → InvalidQuantityException
     * ✗ Cannot order more than available stock → OutOfStockException
     * ✓ If flower already in cart → quantity is incremented (no duplicate row)
     */
    @Transactional
    public Cart addItem(Long userId, Long flowerId, Integer quantity) {

        // ── Validation: quantity must be ≥ 1 ─────────────────────────────────
        if (quantity == null || quantity < 1) {
            throw new InvalidQuantityException("Cannot add negative quantity. Quantity must be at least 1.");
        }

        Cart cart = getOrCreateCart(userId);

        Flower flower = flowerRepository.findById(flowerId)
                .orElseThrow(() -> new ResourceNotFoundException("Flower not found with id: " + flowerId));

        // ── Validation: flower must be in stock ───────────────────────────────
        if (flower.getStock() <= 0) {
            throw new OutOfStockException("'" + flower.getName() + "' is currently out of stock.");
        }

        // ── Validation: requested qty must not exceed available stock ─────────
        if (quantity > flower.getStock()) {
            throw new OutOfStockException(
                    "Cannot order more than available stock. " +
                            "Requested: " + quantity + ", Available: " + flower.getStock() +
                            " for '" + flower.getName() + "'.");
        }

        // ── If flower already in cart → increment quantity ────────────────────
        cartItemRepository.findByCartAndFlower(cart, flower)
                .ifPresentOrElse(
                        existingItem -> {
                            int newQty = existingItem.getQuantity() + quantity;
                            if (newQty > flower.getStock()) {
                                throw new OutOfStockException(
                                        "Cannot add " + quantity + " more. " +
                                                "Only " + flower.getStock() + " units of '" +
                                                flower.getName() + "' available in total.");
                            }
                            existingItem.setQuantity(newQty);
                            cartItemRepository.save(existingItem);
                        },
                        () -> {
                            // New flower in cart → create CartItem row
                            CartItem newItem = CartItem.builder()
                                    .cart(cart)
                                    .flower(flower)
                                    .quantity(quantity)
                                    .build();
                            cart.getItems().add(cartItemRepository.save(newItem));
                        });

        return cartRepository.findById(cart.getId()).orElse(cart);
    }

    // ─── UPDATE QUANTITY ──────────────────────────────────────────────────────

    /**
     * Updates quantity of a cart item. Setting quantity = 0 removes the item.
     *
     * Validation:
     * ✗ Cannot set negative quantity → InvalidQuantityException
     * ✗ Cannot exceed available stock → OutOfStockException
     * ✗ Item must belong to this user's cart → IllegalArgumentException
     */
    @Transactional
    public Cart updateQuantity(Long userId, Long cartItemId, Integer newQuantity) {

        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found with id: " + cartItemId));

        // Security: ensure the item belongs to this user's cart
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("This item does not belong to your cart.");
        }

        if (newQuantity == null || newQuantity < 0) {
            throw new InvalidQuantityException("Cannot add negative quantity.");
        }

        if (newQuantity == 0) {
            cartItemRepository.delete(item);
        } else {
            if (newQuantity > item.getFlower().getStock()) {
                throw new OutOfStockException(
                        "Cannot order more than available stock. " +
                                "Only " + item.getFlower().getStock() + " units available.");
            }
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        }

        return cartRepository.findById(cart.getId()).orElse(cart);
    }

    // ─── REMOVE ITEM ──────────────────────────────────────────────────────────

    @Transactional
    public void removeItem(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found with id: " + cartItemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("This item does not belong to your cart.");
        }

        cartItemRepository.delete(item);
    }

    // ─── CLEAR CART ───────────────────────────────────────────────────────────

    /**
     * Removes all items from the cart. Called by OrderService after order is
     * placed.
     */
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCart(cart);
    }

    // ─── CART SUMMARY ─────────────────────────────────────────────────────────

    /**
     * Returns cart contents with a real-time calculated total.
     * Response includes: cartId, items, totalPrice, itemCount.
     */
    public Map<String, Object> getCartSummary(Long userId) {
        Cart cart = getOrCreateCart(userId);

        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getFlower().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> summary = new HashMap<>();
        summary.put("cartId", cart.getId());
        summary.put("items", cart.getItems());
        summary.put("totalPrice", total);
        summary.put("itemCount", cart.getItems().size());
        return summary;
    }

    // ─── PRIVATE HELPER ───────────────────────────────────────────────────────

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}

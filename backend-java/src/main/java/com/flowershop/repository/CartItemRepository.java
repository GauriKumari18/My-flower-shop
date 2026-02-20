package com.flowershop.repository;

import com.flowershop.model.Cart;
import com.flowershop.model.CartItem;
import com.flowershop.model.Flower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * CART ITEM REPOSITORY
 * --------------------
 * Connects to the "cart_items" table.
 *
 * Used by CartService to:
 * - Find a specific flower inside a cart (to update quantity)
 * - Remove a specific item
 * - Check if an item already exists before adding
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Find a specific flower inside a specific cart.
     * Used when adding a flower that's already in the cart — we increment quantity.
     *
     * Spring generates: SELECT * FROM cart_items WHERE cart_id = ? AND flower_id =
     * ?
     */
    Optional<CartItem> findByCartAndFlower(Cart cart, Flower flower);

    /**
     * Delete all items in a cart at once.
     * Called after an order is placed to clear the user's cart.
     */
    void deleteByCart(Cart cart);
}

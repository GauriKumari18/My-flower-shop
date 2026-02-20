package com.flowershop.repository;

import com.flowershop.model.Cart;
import com.flowershop.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * CART REPOSITORY
 * ---------------
 * Connects to the "carts" table.
 *
 * Each user has at most ONE cart — findByUser is the
 * primary method used in CartService to load or create a cart.
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Find the cart belonging to a specific user.
     * Returns Optional — if no cart exists yet, CartService creates one.
     */
    Optional<Cart> findByUser(User user);

    /**
     * Check if a cart already exists for this user.
     * Avoids creating duplicate carts on concurrent requests.
     */
    boolean existsByUser(User user);
}

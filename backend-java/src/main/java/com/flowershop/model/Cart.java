package com.flowershop.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CART MODEL
 * ----------
 * Each user has ONE active cart stored in the database.
 * A cart holds multiple CartItems (one per flower).
 *
 * Design choice: DB-backed cart (not session-based) so that:
 * - Cart persists across devices and browser closes
 * - Works naturally with our REST + PostgreSQL architecture
 */
@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * One cart belongs to exactly one user.
     * unique=true on the join column enforces one-cart-per-user at DB level.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * All items currently in this cart.
     * CascadeType.ALL → deleting a cart deletes its items too.
     * orphanRemoval → removing item from list deletes it from DB.
     */
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

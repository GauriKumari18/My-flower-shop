package com.flowershop.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * CART ITEM MODEL
 * ---------------
 * Represents ONE flower inside a user's cart with a quantity.
 *
 * Design: if the same flower is added again,
 * the service increments quantity instead of creating a duplicate row.
 *
 * subtotal is NOT stored — it's calculated on the fly:
 * subtotal = flower.price × quantity
 */
@Entity
@Table(name = "cart_items", uniqueConstraints = {
        // Prevent duplicate flower rows in the same cart
        @UniqueConstraint(columnNames = { "cart_id", "flower_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The cart this item belongs to */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    /**
     * The flower being added.
     * We keep a live reference (not a snapshot) because in the cart
     * the price is "current price". Snapshot happens on Order placement.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "flower_id", nullable = false)
    private Flower flower;

    /** How many units of this flower are in the cart */
    @Column(nullable = false)
    private Integer quantity;
}

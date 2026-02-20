package com.flowershop.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * ORDER ITEM MODEL
 * ----------------
 * Represents ONE line item inside a placed Order.
 *
 * Critical design: unitPrice is a SNAPSHOT of the flower's price
 * at the exact moment the order was placed. This means:
 * - If "Red Rose" was $4.99 when ordered → unitPrice = 4.99
 * - Even if admin later changes price to $6.99, this order shows $4.99
 * - Historical orders are never affected by catalog price changes
 *
 * subtotal = unitPrice × quantity (stored, not computed, for audit clarity)
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The order this item belongs to */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Reference to the flower product.
     * Nullable=true because: if admin deletes a flower later,
     * we don't want to also delete the historical order item.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "flower_id", nullable = true)
    private Flower flower;

    /** Snapshot: flower name at time of order (safe even if flower is deleted) */
    @Column(nullable = false)
    private String flowerName;

    /** How many units were ordered */
    @Column(nullable = false)
    private Integer quantity;

    /** Price of ONE unit at the time the order was placed (snapshot) */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /** unitPrice × quantity — stored for easy order total display */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
}

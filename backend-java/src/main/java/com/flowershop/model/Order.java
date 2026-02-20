package com.flowershop.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ORDER MODEL
 * -----------
 * Represents a completed purchase made by a user.
 *
 * Key design decisions:
 * - totalPrice is stored at placement time (snapshot), so later
 * price changes to flowers don't alter historical order totals.
 * - status tracks where the order is in its lifecycle.
 * - items hold OrderItems, each with their own price snapshot.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who placed this order.
     * EAGER fetch since we almost always need user info with an order.
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Snapshot of total price at time of placement.
     * Stored so price changes on flowers don't alter past orders.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    /**
     * Current status of the order.
     * Starts as PENDING when placed.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    /** Optional delivery address left by the customer */
    @Column
    private String deliveryAddress;

    /**
     * All line items in this order.
     * CascadeType.ALL + orphanRemoval keep items in sync with the order.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime orderedAt = LocalDateTime.now();
}

package com.flowershop.model;

/**
 * ORDER STATUS ENUM
 * -----------------
 * Tracks the lifecycle of an order.
 *
 * PENDING → order placed by customer, awaiting admin action
 * CONFIRMED → admin confirmed and accepted the order
 * CANCELLED → cancelled by customer or admin
 * DELIVERED → successfully delivered to the customer
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    DELIVERED
}

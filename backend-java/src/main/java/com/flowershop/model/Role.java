package com.flowershop.model;

/**
 * ROLE ENUM
 * ---------
 * Defines the two types of users in the system.
 *
 * CUSTOMER → can view flowers and place orders
 * ADMIN → can add, update, delete flowers and manage stock
 *
 * Stored as a STRING in the database (not a number),
 * so the database column will show "ADMIN" or "CUSTOMER".
 */
public enum Role {
    CUSTOMER,
    ADMIN
}

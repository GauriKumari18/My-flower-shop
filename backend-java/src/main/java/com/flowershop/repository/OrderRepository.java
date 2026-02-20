package com.flowershop.repository;

import com.flowershop.model.Order;
import com.flowershop.model.OrderStatus;
import com.flowershop.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ORDER REPOSITORY
 * ----------------
 * Connects to the "orders" table.
 *
 * JpaRepository gives: save, findById, findAll, deleteById, etc.
 *
 * Custom methods:
 * - findByUser → customer views their own orders
 * - findAll() → admin views ALL orders (inherited from JpaRepository)
 * - findByStatus → admin filters orders by status
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find all orders placed by a specific user.
     * Used by: GET /api/orders/my-orders (customer)
     *
     * Spring generates: SELECT * FROM orders WHERE user_id = ?
     * ORDER BY ordered_at DESC
     */
    List<Order> findByUserOrderByOrderedAtDesc(User user);

    /**
     * Find all orders with a specific status.
     * Used by: GET /api/admin/orders?status=PENDING (admin filter)
     */
    List<Order> findByStatus(OrderStatus status);
}
